package net.erutobusiness.erutosmobs.entity;

import net.erutobusiness.erutosmobs.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * ミズナギドリ — 嵐の海の主。伝説として海域にごく稀に 1 体。
 *
 * 実物の暮らし（ja.wikipedia「オオミズナギドリ」）を状態にした:
 *   海の上を滑空と羽ばたきで巡る（FLY / GLIDE）／餌を見る（HOVER）／水面へ降りて浮く（DIVE → PADDLE）／
 *   夜は水面で眠る（SLEEP）／助走して飛び立つ（TAKEOFF）。地面での立ち・歩き（STAND / WALK）は
 *   動きだけ用意してあり、浜へ降りる AI は次の版。
 *
 * 動きの名前は mc-model-kit の 13 本（swim / glide / idle / dive / sleep / cry / hover / hurt / faint /
 * stand / walk / paddle / takeoff）。swim が羽ばたき、idle が空中の休止。
 */
public class ShearwaterEntity extends PathfinderMob implements GeoEntity {
    public static final int FLY = 0;
    public static final int GLIDE = 1;
    public static final int DIVE = 2;
    public static final int PADDLE = 3;
    public static final int TAKEOFF = 4;
    public static final int SLEEP = 5;
    public static final int HOVER = 6;
    public static final int STAND = 7;
    public static final int WALK = 8;

    private static final EntityDataAccessor<Integer> STATE =
            SynchedEntityData.defineId(ShearwaterEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation ANIM_FLY = RawAnimation.begin().thenLoop("animation.shearwater.swim");
    private static final RawAnimation ANIM_GLIDE = RawAnimation.begin().thenLoop("animation.shearwater.glide");
    private static final RawAnimation ANIM_HOVER = RawAnimation.begin().thenLoop("animation.shearwater.hover");
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.shearwater.idle");
    private static final RawAnimation ANIM_DIVE = RawAnimation.begin().thenPlayAndHold("animation.shearwater.dive");
    private static final RawAnimation ANIM_PADDLE = RawAnimation.begin().thenLoop("animation.shearwater.paddle");
    private static final RawAnimation ANIM_SLEEP = RawAnimation.begin().thenLoop("animation.shearwater.sleep");
    private static final RawAnimation ANIM_TAKEOFF = RawAnimation.begin().thenPlayAndHold("animation.shearwater.takeoff");
    private static final RawAnimation ANIM_STAND = RawAnimation.begin().thenLoop("animation.shearwater.stand");
    private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("animation.shearwater.walk");
    private static final RawAnimation ANIM_CRY = RawAnimation.begin().thenPlay("animation.shearwater.cry");
    private static final RawAnimation ANIM_HURT = RawAnimation.begin().thenPlay("animation.shearwater.hurt");
    private static final RawAnimation ANIM_FAINT = RawAnimation.begin().thenPlayAndHold("animation.shearwater.faint");

    /** 動きの長さ（tick）。キットの秒 × 20。 */
    private static final int DIVE_TICKS = 24;
    private static final int TAKEOFF_TICKS = 28;
    private static final int FAINT_TICKS = 40;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int stateTimer;
    private int flightModeTimer = 100;
    private int restCooldown = 1800;
    private int paddleTime = 400;
    private int cryCooldown = 400;
    private double surfaceY = Double.NaN;

    public ShearwaterEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0f);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, -1.0f);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, -1.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.9)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    /**
     * 湧きの稀さ。海面（水）・空が見える・25 回に 1 回・192 ブロック以内に同族が居ない。
     * スポーンエッグとスポナーはそのまま通す。
     */
    public static boolean checkShearwaterSpawn(EntityType<ShearwaterEntity> type, ServerLevelAccessor level,
                                               MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (reason == MobSpawnType.SPAWN_EGG || reason == MobSpawnType.SPAWNER || reason == MobSpawnType.COMMAND) {
            return true;
        }
        if (!level.getFluidState(pos).is(FluidTags.WATER) || !level.canSeeSky(pos)) {
            return false;
        }
        if (random.nextInt(25) != 0) {
            return false;
        }
        return level.getEntitiesOfClass(ShearwaterEntity.class, new AABB(pos).inflate(192.0)).isEmpty();
    }

    // ---------------------------------------------------------------- 骨組み

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, FLY);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new SeaWanderGoal(this));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 24.0f));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData data,
                                        @Nullable net.minecraft.nbt.CompoundTag tag) {
        if (this.isInWater()) {
            enterPaddle();
        } else {
            setState(FLY);
        }
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    public int getState() {
        return this.entityData.get(STATE);
    }

    public void setState(int state) {
        if (getState() != state) {
            this.entityData.set(STATE, state);
            this.stateTimer = 0;
        }
    }

    public boolean isFlying() {
        int s = getState();
        return s == FLY || s == GLIDE || s == HOVER || s == TAKEOFF || s == DIVE;
    }

    public boolean isOnWater() {
        int s = getState();
        return s == PADDLE || s == SLEEP;
    }

    // ---------------------------------------------------------------- 毎 tick（サーバ）

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.stateTimer++;
        int st = getState();
        switch (st) {
            case FLY, GLIDE -> tickFlight(st);
            case HOVER -> tickHover();
            case DIVE -> tickDive();
            case PADDLE, SLEEP -> tickOnWater(st);
            case TAKEOFF -> tickTakeoff();
            case STAND, WALK -> {
                // 次の版（浜へ降りる AI）。今は着水の流れへ戻す
                setState(FLY);
            }
            default -> setState(FLY);
        }
        if (--this.cryCooldown <= 0 && st != SLEEP && st != DIVE && st != TAKEOFF) {
            this.cryCooldown = 600 + this.random.nextInt(1400);
            triggerAnim("main", "cry");
            this.playSound(ModSounds.SHEARWATER_CRY.get(), 2.0f, 0.95f + this.random.nextFloat() * 0.1f);
        }
    }

    private void tickFlight(int st) {
        // 羽ばたきと滑空を交互に（実物: 主に滑翔して、ゆっくりとした羽ばたきを交える）
        if (--this.flightModeTimer <= 0) {
            boolean toGlide = st == FLY;
            setState(toGlide ? GLIDE : FLY);
            this.flightModeTimer = toGlide ? 80 + this.random.nextInt(120) : 50 + this.random.nextInt(50);
        }
        if (this.stateTimer > 30 && this.getNavigation().isDone()
                && this.getDeltaMovement().horizontalDistanceSqr() < 0.0004) {
            setState(HOVER);
        }
        if (--this.restCooldown <= 0 && wantsToRest()) {
            beginDive();
        }
    }

    private void tickHover() {
        if (this.stateTimer > 60 && (this.getDeltaMovement().horizontalDistanceSqr() > 0.002
                || !this.getNavigation().isDone())) {
            setState(FLY);
            this.flightModeTimer = 100;
        }
        if (this.stateTimer > 200) {
            setState(GLIDE);
            this.flightModeTimer = 120;
        }
    }

    private void tickDive() {
        this.getNavigation().stop();
        Vec3 forward = this.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(0.12);
        this.setDeltaMovement(forward.add(0.0, -0.28, 0.0));
        if (this.isInWater() || (!Double.isNaN(this.surfaceY) && this.getY() <= this.surfaceY + 0.2)
                || this.stateTimer > DIVE_TICKS * 3) {
            enterPaddle();
        }
    }

    private void tickOnWater(int st) {
        if (!holdOnSurface()) {
            beginTakeoff();
            return;
        }
        if (st == PADDLE && this.level().isNight() && this.stateTimer > 100) {
            setState(SLEEP);
            return;
        }
        if (st == SLEEP) {
            if (this.level().isDay() && this.stateTimer > 200) {
                setState(PADDLE);
                this.paddleTime = 200 + this.random.nextInt(400);
            }
            return;
        }
        Player near = this.level().getNearestPlayer(this, 6.0);
        if (this.stateTimer > this.paddleTime || (near != null && !near.isSpectator() && !near.isCreative())) {
            beginTakeoff();
        }
    }

    private void tickTakeoff() {
        this.setNoGravity(true);
        Vec3 forward = this.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
        double up = this.stateTimer < 12 ? 0.04 : 0.16;
        this.setDeltaMovement(forward.scale(0.22).add(0.0, up, 0.0));
        if (this.stateTimer > TAKEOFF_TICKS) {
            setState(FLY);
            this.flightModeTimer = 100;
            this.restCooldown = 2400 + this.random.nextInt(2400);
        }
    }

    /** 水面へ降りる気になるか: 真下が水で、近くに人が居ない。 */
    private boolean wantsToRest() {
        if (this.level().getNearestPlayer(this, 16.0) != null) {
            return false;
        }
        double top = surfaceBelow();
        return !Double.isNaN(top);
    }

    private void beginDive() {
        this.surfaceY = surfaceBelow();
        if (Double.isNaN(this.surfaceY)) {
            this.restCooldown = 200;
            return;
        }
        setState(DIVE);
        this.getNavigation().stop();
    }

    private void enterPaddle() {
        setState(PADDLE);
        this.setNoGravity(false);
        this.getNavigation().stop();
        this.paddleTime = 300 + this.random.nextInt(600);
        this.surfaceY = surfaceBelow();
    }

    private void beginTakeoff() {
        setState(TAKEOFF);
        this.setNoGravity(true);
        this.getNavigation().stop();
    }

    /** 真下の水面の高さ（水でなければ NaN）。 */
    private double surfaceBelow() {
        BlockPos here = this.blockPosition();
        int top = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, here.getX(), here.getZ());
        BlockPos water = new BlockPos(here.getX(), top - 1, here.getZ());
        if (!this.level().getFluidState(water).is(FluidTags.WATER)) {
            return Double.NaN;
        }
        return top;
    }

    /** 水面に浮いたまま保つ。水が無ければ false。 */
    private boolean holdOnSurface() {
        double top = surfaceBelow();
        if (Double.isNaN(top)) {
            return false;
        }
        this.surfaceY = top;
        double want = top - 0.45;                      // 胴が水面に沈む分
        double y = this.getY() + (want - this.getY()) * 0.3;
        Vec3 v = this.getDeltaMovement();
        this.setDeltaMovement(v.x * 0.9, 0.0, v.z * 0.9);
        this.setPos(this.getX(), y, this.getZ());
        return true;
    }

    // ---------------------------------------------------------------- 物理

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state,
                                   BlockPos pos) {
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    // ---------------------------------------------------------------- 被弾・死

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean ok = super.hurt(source, amount);
        if (ok && !this.level().isClientSide && this.isAlive()) {
            triggerAnim("main", "hurt");
            int st = getState();
            if (st == PADDLE || st == SLEEP || st == DIVE) {
                beginTakeoff();
            }
        }
        return ok;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide) {
            triggerAnim("main", "faint");
        }
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime >= FAINT_TICKS && !this.level().isClientSide()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    // ---------------------------------------------------------------- 音

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;                                    // 鳴きは cry の動きと一緒に出す
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SHEARWATER_HURT.get();
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SHEARWATER_DEATH.get();
    }

    // ---------------------------------------------------------------- GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, this::mainAnimation)
                .triggerableAnim("cry", ANIM_CRY)
                .triggerableAnim("hurt", ANIM_HURT)
                .triggerableAnim("faint", ANIM_FAINT));
    }

    private PlayState mainAnimation(AnimationState<ShearwaterEntity> state) {
        RawAnimation anim = switch (getState()) {
            case GLIDE -> ANIM_GLIDE;
            case HOVER -> ANIM_HOVER;
            case DIVE -> ANIM_DIVE;
            case PADDLE -> ANIM_PADDLE;
            case SLEEP -> ANIM_SLEEP;
            case TAKEOFF -> ANIM_TAKEOFF;
            case STAND -> ANIM_STAND;
            case WALK -> ANIM_WALK;
            default -> ANIM_FLY;
        };
        state.setAndContinue(anim);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ---------------------------------------------------------------- 海の上を巡る

    /** 海の上の点を選んで飛ぶ。水面の 6〜18 上。水面で休んでいる間は動かない。 */
    static class SeaWanderGoal extends Goal {
        private final ShearwaterEntity bird;
        private int idle;

        SeaWanderGoal(ShearwaterEntity bird) {
            this.bird = bird;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!this.bird.isFlying() || this.bird.getState() == DIVE || this.bird.getState() == TAKEOFF) {
                return false;
            }
            if (!this.bird.getNavigation().isDone()) {
                return false;
            }
            return ++this.idle > 20 + this.bird.random.nextInt(40);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            this.idle = 0;
            Vec3 target = pickTarget();
            if (target != null) {
                double speed = this.bird.getState() == GLIDE ? 0.9 : 1.1;
                this.bird.getNavigation().moveTo(target.x, target.y, target.z, speed);
            }
        }

        @Nullable
        private Vec3 pickTarget() {
            Level level = this.bird.level();
            RandomSource random = this.bird.random;
            for (int i = 0; i < 10; i++) {
                int dx = random.nextInt(49) - 24;
                int dz = random.nextInt(49) - 24;
                int x = this.bird.blockPosition().getX() + dx;
                int z = this.bird.blockPosition().getZ() + dz;
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                boolean water = level.getFluidState(new BlockPos(x, top - 1, z)).is(FluidTags.WATER);
                int y = top + 6 + random.nextInt(13);
                if (water || i == 9) {
                    return new Vec3(x + 0.5, y, z + 0.5);
                }
            }
            return null;
        }
    }
}
