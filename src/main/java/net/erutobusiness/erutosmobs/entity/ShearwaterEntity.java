package net.erutobusiness.erutosmobs.entity;

import net.erutobusiness.erutosmobs.ErutosMobsConfig;
import net.erutobusiness.erutosmobs.registry.ModParticles;
import net.erutobusiness.erutosmobs.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Pose;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
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
import java.util.UUID;

/**
 * ミズナギドリ — 嵐の海の主。伝説として海域にごく稀に 1 体。
 *
 * 実物の暮らし（ja.wikipedia「オオミズナギドリ」）を状態にした:
 *   海の上を滑空と羽ばたきで巡る（FLY / GLIDE）／餌を見る（HOVER）／水面へ降りて浮く（DIVE → PADDLE）／
 *   夜は水面で眠る（SLEEP）／助走して飛び立つ（TAKEOFF）／ときどき水辺の浜へ降りて立ち、歩く（LAND → STAND / WALK）。
 * 人とのかかわり（実物は漁船に付いて飛び、捨てられた魚を拾う）: 海を行くボートの横に並んで飛ぶ（{@link FollowBoatGoal}）／
 *   水面に投げた生の魚へ飛び込んで食べ、投げた人を 5 分信用する（{@link FishLureGoal}）。
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
    public static final int LAND = 9;

    private static final EntityDataAccessor<Integer> STATE =
            SynchedEntityData.defineId(ShearwaterEntity.class, EntityDataSerializers.INT);
    /** 滑空の傾き: −1 左へ、0 水平、+1 右へ（向きの変化から決める） */
    private static final EntityDataAccessor<Integer> BANK =
            SynchedEntityData.defineId(ShearwaterEntity.class, EntityDataSerializers.INT);
    private float bankFilter;
    /**
     * 帯電（雷を受けた・嵐で雷を呼んだ後の 1 分）。光る層が最大近くまで上がり、翼から火花が出る。
     * 残りの tick はサーバだけが持ち、クライアントへは入り切りだけを送る（毎 tick 送らない）。
     */
    private static final EntityDataAccessor<Boolean> CHARGED =
            SynchedEntityData.defineId(ShearwaterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int CHARGE_TICKS = 1200;
    private int chargeTicks;
    /** 鳴いたことをクライアントへ知らせる番号（光る層が一瞬強まる）。バニラの使う番号（〜67）と重ならない値 */
    private static final byte EVENT_CRY = (byte) 101;
    /** 鳴きの動き（cry）の長さ＝1.2 秒 */
    private static final int CRY_TICKS = 24;
    private int cryGlowTicks;
    private final ShearwaterEffects effects = new ShearwaterEffects(this);
    /**
     * 描いた骨から取った翼の点（実体から見た位置・世界の向き・ブロック単位。並びは {@code ShearwaterLocators.LIVE_*}）。
     * クライアントだけ。`ShearwaterRenderer` が描くたびに書き、粒子（{@link ShearwaterEffects}）が読む
     */
    private double[][] livePoints;
    private int livePointsTick = -100;
    /** 滑空を保たせる残り（{@link ShearPassGoal} が波を切る間、羽ばたき・休みへ切り替えない） */
    private int glideHold;
    /** 次に波を切ってよいまでの tick（{@link ShearPassGoal}） */
    int shearCooldown = 600;
    /** 次に船へ付いて飛んでよいまでの tick（{@link FollowBoatGoal}） */
    int followCooldown = 400;
    /** 魚を食べさせてくれた人（{@link FishLureGoal}）。その人の船には付きやすい。{@link #TRUST_TICKS} で忘れる */
    @Nullable
    private UUID trustedPlayer;
    private int trustTicks;
    private static final int TRUST_TICKS = 6000;
    /** 魚を食べた直後、人が近くても飛び立たない残り（食べているところを見せる） */
    private int calmTicks;
    /**
     * 上り下りの傾き（度・正で上り＝鼻先が上）。クライアントだけで、位置の変化から決める（{@link #tick}）。
     * 描画（ShearwaterModel）が胴の回転に足す。ルギアの `q.pitch_tilt`（±45° で止める）と、
     * タイヨウチョウの `birdPitch`（上下の速さ × 57.3）と同じ役。
     */
    public float tilt;
    public float tiltO;
    private static final float MAX_TILT = 35.0f;

    private static final RawAnimation ANIM_FLY = RawAnimation.begin().thenLoop("animation.shearwater.swim");
    private static final RawAnimation ANIM_GLIDE = RawAnimation.begin().thenLoop("animation.shearwater.glide");
    private static final RawAnimation ANIM_GLIDE_L = RawAnimation.begin().thenLoop("animation.shearwater.glide_bank_l");
    private static final RawAnimation ANIM_GLIDE_R = RawAnimation.begin().thenLoop("animation.shearwater.glide_bank_r");
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
    /** 離陸は 2.0 秒（2026-09-28。羽ばたきを大きさに合わせて遅くしたので、打つ回数を保つために 1.4 → 2.0） */
    private static final int TAKEOFF_TICKS = 40;
    /** 羽ばたきの 1 打（tick）。キットの `LEGEND_SCALE`（飛ぶ 0.85 秒・上る 0.77 秒）× 20 */
    private static final int FLAP_TICKS = 17;
    private static final int CLIMB_FLAP_TICKS = 15;
    private static final int FAINT_TICKS = 40;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int stateTimer;
    private int flightModeTimer = 100;
    private int restCooldown = 1800;
    private int paddleTime = 400;
    private int cryCooldown = 400;
    private double surfaceY = Double.NaN;
    @Nullable
    private BlockPos shoreTarget;
    private int shoreTime = 300;
    private float walkYaw;
    private int walkTime = 40;

    public ShearwaterEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new ShearwaterMoveControl(this);
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
        int oneIn = ErutosMobsConfig.SHEARWATER_SPAWN_ONE_IN.get();
        if (oneIn > 1 && random.nextInt(oneIn) != 0) {
            return false;
        }
        double minDist = ErutosMobsConfig.SHEARWATER_MIN_DISTANCE.get();
        return minDist <= 0
                || level.getEntitiesOfClass(ShearwaterEntity.class, new AABB(pos).inflate(minDist)).isEmpty();
    }

    // ---------------------------------------------------------------- 消え方・見え方・大きさ

    /**
     * 伝説なので、水の生き物の既定（128 ブロックで即消える）より粘る。
     * 全員が `despawnDistance`（既定 256）より遠いときだけ消える。近づいて追える距離で消えない。
     */
    @Override
    public boolean removeWhenFarAway(double distSqr) {
        double d = ErutosMobsConfig.SHEARWATER_DESPAWN_DISTANCE.get();
        return distSqr > d * d;
    }

    /**
     * 描画の当たり判定を翼の分だけ広げる（0.6 倍で翼幅 約 12 ブロック）。
     * これが無いと、当たり判定（幅 3）が画面の外に出た瞬間に翼ごと消える。
     */
    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(6.5, 2.5, 6.5);
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.72f;
    }

    @Override
    protected float getSoundVolume() {
        return 1.4f;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypeTags.IS_FALL)
                || super.isInvulnerableTo(source);
    }

    // ---------------------------------------------------------------- 保存

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ShearwaterState", getState());
        tag.putInt("RestCooldown", this.restCooldown);
        tag.putInt("ChargeTicks", this.chargeTicks);
        tag.putInt("ShearCooldown", this.shearCooldown);
        tag.putInt("FollowCooldown", this.followCooldown);
        if (this.trustedPlayer != null && this.trustTicks > 0) {
            tag.putUUID("TrustedPlayer", this.trustedPlayer);
            tag.putInt("TrustTicks", this.trustTicks);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int st = tag.getInt("ShearwaterState");
        // ⚠ 途中の動き（降下・離陸・着地）は保存しない。飛んでいる状態か浮いている状態へ丸める
        if (st == PADDLE || st == SLEEP) {
            setState(st);
            this.setNoGravity(false);
        } else if (st == STAND || st == WALK) {
            setState(STAND);
            this.setNoGravity(false);
            this.shoreTime = 100;
        } else {
            setState(FLY);
            this.setNoGravity(true);
        }
        // ⚠ /summon は中身の無い NBT でも読みに来る。無いときは既定のまま（0 → 200 に丸めると、召喚して 10 秒で降りた）
        if (tag.contains("RestCooldown")) {
            this.restCooldown = Math.max(200, tag.getInt("RestCooldown"));
        }
        this.chargeTicks = Mth.clamp(tag.getInt("ChargeTicks"), 0, CHARGE_TICKS);
        if (tag.contains("ShearCooldown")) {
            this.shearCooldown = Math.max(0, tag.getInt("ShearCooldown"));
        }
        if (tag.contains("FollowCooldown")) {
            this.followCooldown = Math.max(0, tag.getInt("FollowCooldown"));
        }
        if (tag.hasUUID("TrustedPlayer")) {
            this.trustedPlayer = tag.getUUID("TrustedPlayer");
            this.trustTicks = Mth.clamp(tag.getInt("TrustTicks"), 0, TRUST_TICKS);
        }
        this.entityData.set(CHARGED, this.chargeTicks > 0);
    }

    // ---------------------------------------------------------------- 骨組み

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, FLY);
        this.entityData.define(BANK, 0);
        this.entityData.define(CHARGED, false);
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
        // ⚠ 同じ優先度の目標は互いに割り込まない（`WrappedGoal.canBeReplacedBy` は優先度の数が大きい方だけを譲らせる）。
        //   魚（0）は船・波切り・巡る目標に割り込む。船（1）と波切り（1）は、走っている方が終わるまで待つ
        this.goalSelector.addGoal(0, new FishLureGoal(this));
        this.goalSelector.addGoal(1, new FollowBoatGoal(this));
        this.goalSelector.addGoal(1, new ShearPassGoal(this));
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
            if (state != FLY && state != GLIDE && state != HOVER && state != LAND
                    && this.moveControl instanceof ShearwaterMoveControl mc) {
                mc.halt();
            }
        }
    }

    /** 波を切っている最中か（{@link ShearPassGoal}。このときは水面すれすれまで降りてよい） */
    public boolean isShearing() {
        return this.glideHold > 0;
    }

    public boolean isFlying() {
        int s = getState();
        return s == FLY || s == GLIDE || s == HOVER || s == TAKEOFF || s == DIVE || s == LAND;
    }

    public boolean isOnShore() {
        int s = getState();
        return s == STAND || s == WALK;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            // 上り下りの傾き。この tick の位置の変化から角度を出し、急に振れないよう 15% ずつ寄せる
            this.tiltO = this.tilt;
            float target = 0.0f;
            int st = getState();
            if (st == FLY || st == GLIDE || st == HOVER) {
                double dx = this.getX() - this.xo;
                double dz = this.getZ() - this.zo;
                double dy = this.getY() - this.yo;
                double h = Math.max(Math.sqrt(dx * dx + dz * dz), 0.08);
                target = net.minecraft.util.Mth.clamp((float) Math.toDegrees(Math.atan2(dy, h)), -MAX_TILT, MAX_TILT);
            }
            this.tilt += (target - this.tilt) * 0.15f;
            if (this.cryGlowTicks > 0) {
                this.cryGlowTicks--;
            }
            if (this.isAlive()) {
                this.effects.tick();
            }
        }
    }

    // ---------------------------------------------------------------- 表現（光・雷・鳴き）

    public int getBank() {
        return this.entityData.get(BANK);
    }

    public boolean isCharged() {
        return this.entityData.get(CHARGED);
    }

    /** 描いた骨から取った翼の点を受け取る（`ShearwaterRenderer` が描くたびに呼ぶ）。 */
    public void setLivePoints(double[][] points) {
        this.livePoints = points;
        this.livePointsTick = this.tickCount;
    }

    /** 描いた骨から取った翼の点。3 tick より古ければ（画面の外にいて描かれていない）null。 */
    @Nullable
    double[][] livePoints() {
        return this.tickCount - this.livePointsTick <= 3 ? this.livePoints : null;
    }

    /** 鳴いた直後の光の上乗せ（0〜0.6）。鳴きの動きの 1.2 秒で山を描く。クライアントだけで意味を持つ */
    public float cryGlow(float partialTick) {
        if (this.cryGlowTicks <= 0) {
            return 0.0f;
        }
        float p = Mth.clamp((CRY_TICKS - this.cryGlowTicks + partialTick) / CRY_TICKS, 0.0f, 1.0f);
        return 0.6f * Mth.sin(p * Mth.PI);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_CRY) {
            this.cryGlowTicks = CRY_TICKS;
        } else {
            super.handleEntityEvent(id);
        }
    }

    /** 鳴く（動き・声・光の合図を一度に）。サーバで呼ぶ */
    private void cry() {
        triggerAnim("main", "cry");
        this.playSound(ModSounds.SHEARWATER_CRY.get(), 2.0f, 0.95f + this.random.nextFloat() * 0.1f);
        this.level().broadcastEntityEvent(this, EVENT_CRY);
    }

    /** 帯電させる。入ったばかりでなければ鳴き、傷が少し癒える（嵐の主は雷で力を得る） */
    private void charge() {
        boolean fresh = this.chargeTicks < CHARGE_TICKS - 40;   // 1 本の雷は数 tick 続けて当たるので、2 回目以降は数えない
        this.chargeTicks = CHARGE_TICKS;
        this.entityData.set(CHARGED, true);
        if (fresh) {
            this.heal(10.0f);
            cry();
            this.cryCooldown = Math.max(this.cryCooldown, 400);
        }
    }

    /**
     * 雷を受けても傷まず、燃えず、帯電する（バニラは火を付けて 5 の傷。`Entity.thunderHit`）。
     * ⚠ 鳥が雷に当たるのは稀なので、嵐の中では自分で呼ぶ（{@link #customServerAiStep}）。
     */
    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {
        this.clearFire();
        charge();
    }

    /** 時刻で見た夜（日の入り 13000〜日の出 23000）。空の暗さ（雨・雷雨で下がる）は見ない */
    private boolean isNightTime() {
        long t = this.level().getDayTime() % 24000L;
        return t >= 13000L && t < 23000L;
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
        if (this.followCooldown > 0) {
            this.followCooldown--;
        }
        if (this.trustTicks > 0 && --this.trustTicks == 0) {
            this.trustedPlayer = null;
        }
        if (this.calmTicks > 0) {
            this.calmTicks--;
        }
        int st = getState();
        switch (st) {
            case FLY, GLIDE -> tickFlight(st);
            case HOVER -> tickHover();
            case DIVE -> tickDive();
            case PADDLE, SLEEP -> tickOnWater(st);
            case TAKEOFF -> tickTakeoff();
            case LAND -> tickLanding();
            case STAND, WALK -> tickOnShore(st);
            default -> setState(FLY);
        }
        // ⚠ 実物「ほとんど海上で鳴くことはないが、夜間の営巣地では鳴き声や翼の音で騒がしくなる」
        //   （ja.wikipedia）。飛んでいる間は 4 回に 1 回だけ、浮いている・立っている・夜・雷雨は毎回鳴く。
        if (--this.cryCooldown <= 0 && st != SLEEP && st != DIVE && st != TAKEOFF && st != LAND) {
            this.cryCooldown = 800 + this.random.nextInt(1600);
            boolean loud = isNightTime() || this.level().isThundering();
            boolean quietAtSea = isFlying() && !loud && this.random.nextInt(4) != 0;
            if (!quietAtSea) {
                cry();
            }
        }
        // 帯電の残り
        if (this.chargeTicks > 0 && --this.chargeTicks == 0) {
            this.entityData.set(CHARGED, false);
        }
        // 嵐の主: 雷雨の中を飛んでいると、ときどき自分へ雷を呼ぶ（既定は 1 tick に 1/1800＝平均 90 秒に 1 回。
        //   設定 `stormLightningOneIn`、0 で無し）。⚠ 見た目だけの雷（setVisualOnly）なので、火も付かず誰も傷まない
        //   （`LightningBolt.tick` は visualOnly のとき当たり判定と着火を飛ばす）
        int stormOneIn = ErutosMobsConfig.SHEARWATER_STORM_LIGHTNING_ONE_IN.get();
        if (stormOneIn > 0 && st != DIVE && st != LAND && isFlying() && this.chargeTicks == 0
                && this.level().isThundering() && this.random.nextInt(stormOneIn) == 0
                && this.level().canSeeSky(this.blockPosition().above(2))) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(this.level());
            if (bolt != null) {
                bolt.moveTo(this.getX(), this.getY() + 1.0, this.getZ());
                bolt.setVisualOnly(true);
                this.level().addFreshEntity(bolt);
                charge();
            }
        }
    }

    private void tickFlight(int st) {
        // 羽音は 1 打に 1 回（0.85 秒＝17 tick）。大きな翼なので低く大きめに（2026-09-28。前は 4 Hz の小鳥の拍だった）
        if (st == FLY && this.stateTimer % FLAP_TICKS == 2) {
            this.playSound(ModSounds.SHEARWATER_FLAP.get(), 0.9f, 0.75f + this.random.nextFloat() * 0.15f);
        }
        // 傾き（bank）: 向きの変化から左右を決め、滑空の左右 2 本を選ばせる
        float dyaw = net.minecraft.util.Mth.wrapDegrees(this.getYRot() - this.yRotO);
        this.bankFilter = this.bankFilter * 0.85f + dyaw * 0.15f;
        int bank = this.bankFilter > 0.6f ? 1 : (this.bankFilter < -0.6f ? -1 : 0);
        if (bank != this.entityData.get(BANK)) {
            this.entityData.set(BANK, bank);
        }
        if (this.shearCooldown > 0) {
            this.shearCooldown--;
        }
        // 波を切っている間（ShearPassGoal）は滑空のまま。羽ばたき・止まる・休むへ切り替えない
        if (this.glideHold > 0) {
            this.glideHold--;
            if (st != GLIDE) {
                setState(GLIDE);
            }
            return;
        }
        // 羽ばたくか滑るか（実物: 主に滑翔して、ゆっくりとした羽ばたきを交える）。
        // 上るとき・遅いときは羽ばたく。それ以外は滑空を基本に、5〜12 秒ごとに 2〜3 打だけ羽ばたく。
        // 滑空へ移るのは打ち終わり（1 打 17 tick の区切り）で、翼を振り上げた途中では切らない
        Vec3 v = this.getDeltaMovement();
        boolean needPower = v.y > 0.035 || v.horizontalDistance() < 0.15;
        this.flightModeTimer--;
        if (st == GLIDE) {
            // 滑空は最低 1.5 秒続ける（0.5 秒で羽ばたきへ戻ると、翼を広げ切らないうちに打ち始めてちらつく）。
            // ただしほとんど止まりかけ（0.1 未満）なら、すぐ羽ばたく
            boolean mustFlap = v.horizontalDistance() < 0.1;
            if ((needPower && (this.stateTimer > 30 || mustFlap)) || this.flightModeTimer <= 0) {
                setState(FLY);
                this.flightModeTimer = FLAP_TICKS * (2 + this.random.nextInt(2));
            }
        } else if (!needPower && this.flightModeTimer <= 0 && this.stateTimer % FLAP_TICKS == 0) {
            setState(GLIDE);
            this.flightModeTimer = 100 + this.random.nextInt(140);
        }
        if (--this.restCooldown <= 0) {
            // ⚠ 3 回に 1 回は浜へ（実物は陸が下手なので、水面で休むほうを多く）。雷雨の間は休まない（嵐の主の出番）
            if (this.level().isThundering()) {
                this.restCooldown = 200;
            } else if (this.random.nextInt(3) == 0 && wantsToLand()) {
                beginLanding();
            } else if (wantsToRest()) {
                beginDive();
            } else {
                this.restCooldown = 200;
            }
        }
    }

    // ---------------------------------------------------------------- 浜へ降りる

    /** 20 ブロック以内に、空が見えて水が近い砂か草の地面が在れば、その点を持つ。 */
    private boolean wantsToLand() {
        if (this.level().getNearestPlayer(this, 16.0) != null) {
            return false;
        }
        Level level = this.level();
        BlockPos here = this.blockPosition();
        for (int i = 0; i < 8; i++) {
            int x = here.getX() + this.random.nextInt(41) - 20;
            int z = here.getZ() + this.random.nextInt(41) - 20;
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos ground = new BlockPos(x, top - 1, z);
            BlockState gs = level.getBlockState(ground);
            boolean beachy = gs.is(BlockTags.SAND) || gs.is(Blocks.GRASS_BLOCK) || gs.is(Blocks.GRAVEL);
            if (!beachy || !level.getFluidState(ground).isEmpty()) {
                continue;
            }
            boolean waterNear = false;
            for (int dx = -6; dx <= 6 && !waterNear; dx += 3) {
                for (int dz = -6; dz <= 6; dz += 3) {
                    int t2 = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
                    if (level.getFluidState(new BlockPos(x + dx, t2 - 1, z + dz)).is(FluidTags.WATER)) {
                        waterNear = true;
                        break;
                    }
                }
            }
            if (waterNear) {
                this.shoreTarget = new BlockPos(x, top, z);
                return true;
            }
        }
        return false;
    }

    private void beginLanding() {
        setState(LAND);
    }

    private void tickLanding() {
        BlockPos t = this.shoreTarget;
        if (t == null) {
            setState(FLY);
            return;
        }
        double dx = t.getX() + 0.5 - this.getX();
        double dz = t.getZ() + 0.5 - this.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist >= 2.5 && this.stateTimer <= 120) {
            // 降りる点の 3 上へ向かう。曲がる半径があるので、行き過ぎたら輪を描いて戻ってくる
            this.moveControl.setWantedPosition(t.getX() + 0.5, t.getY() + 3.0, t.getZ() + 0.5, 0.8);
        } else {
            // 真上に来たら（か、時間切れなら）そのまま降りる
            if (this.moveControl instanceof ShearwaterMoveControl mc) {
                mc.halt();
            }
            Vec3 v = dist > 0.2 ? new Vec3(dx / dist * 0.08, -0.18, dz / dist * 0.08) : new Vec3(0.0, -0.18, 0.0);
            this.setDeltaMovement(v);
            this.setNoGravity(false);
            if (this.onGround() || this.getY() <= t.getY() + 0.05) {
                setState(STAND);
                this.shoreTime = 200 + this.random.nextInt(500);
                this.setDeltaMovement(Vec3.ZERO);
            }
        }
        if (this.stateTimer > 400 || this.isInWater()) {
            beginTakeoff();
        }
    }

    private void tickOnShore(int st) {
        this.setNoGravity(false);
        this.getNavigation().stop();
        Player near = this.level().getNearestPlayer(this, 8.0);
        boolean disturbed = near != null && !near.isSpectator() && !near.isCreative();
        if (disturbed || this.stateTimer > this.shoreTime || this.isInWater()) {
            beginTakeoff();
            return;
        }
        if (st == STAND) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
            if (this.stateTimer > 60 && this.random.nextInt(80) == 0) {
                this.walkYaw = this.getYRot() + (this.random.nextFloat() - 0.5f) * 120.0f;
                this.walkTime = 40 + this.random.nextInt(50);
                int keep = this.shoreTime - this.stateTimer;
                setState(WALK);
                this.shoreTime = keep;
            }
            return;
        }
        // WALK: 向きをゆっくり合わせて、その向きへ歩く（よちよち。速さは飛ぶときの 1/8）
        float yaw = net.minecraft.util.Mth.approachDegrees(this.getYRot(), this.walkYaw, 4.0f);
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        Vec3 fwd = Vec3.directionFromRotation(0.0f, yaw).multiply(1.0, 0.0, 1.0).normalize().scale(0.045);
        this.setDeltaMovement(fwd.x, this.getDeltaMovement().y, fwd.z);
        if (this.horizontalCollision || this.stateTimer > this.walkTime) {
            int keep = this.shoreTime - this.stateTimer;
            setState(STAND);
            this.shoreTime = Math.max(60, keep);
        }
    }

    /** 止まって羽ばたく（餌を探す）。始めと終わりは {@link SeaWanderGoal} が決める。ここは羽音と、取り残されたときの戻りだけ */
    private void tickHover() {
        if (this.stateTimer % CLIMB_FLAP_TICKS == 2) {
            this.playSound(ModSounds.SHEARWATER_FLAP.get(), 0.7f, 0.85f + this.random.nextFloat() * 0.15f);
        }
        if (this.stateTimer > 160) {
            setState(FLY);
            this.flightModeTimer = FLAP_TICKS * 2;
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
        // 雷雨が来たら、眠っていても 5 秒で飛び立つ（嵐の主の出番）
        if (this.level().isThundering() && this.stateTimer > 100) {
            beginTakeoff();
            return;
        }
        // ⚠ 眠る・起きるは時刻で決める。`Level.isNight()` は空の暗さで決まり、雷雨の昼も夜と数える
        //   （2026-09-28 に、昼に固定した開発サーバで雷雨にしたら水面で眠った）
        if (st == PADDLE && isNightTime() && this.stateTimer > 100) {
            setState(SLEEP);
            return;
        }
        if (st == SLEEP) {
            if (!isNightTime() && this.stateTimer > 200) {
                setState(PADDLE);
                this.paddleTime = 200 + this.random.nextInt(400);
            }
            return;
        }
        Player near = this.level().getNearestPlayer(this, 6.0);
        boolean disturbed = near != null && !near.isSpectator() && !near.isCreative() && this.calmTicks <= 0;
        if (this.stateTimer > this.paddleTime || disturbed) {
            beginTakeoff();
        }
    }

    private void tickTakeoff() {
        this.setNoGravity(true);
        // 羽音は打ち下ろしの拍（上る 0.77 秒＝15 tick。動きは 0.35 秒から打ち始める）
        if (this.stateTimer >= 8 && (this.stateTimer - 8) % CLIMB_FLAP_TICKS == 0 && this.stateTimer < TAKEOFF_TICKS - 4) {
            this.playSound(ModSounds.SHEARWATER_FLAP.get(), 1.2f, 0.8f + this.random.nextFloat() * 0.15f);
        }
        Vec3 forward = this.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
        // 最初の 0.8 秒は水面を走る（ほぼ上がらない）。そこから昇る（動きの側では胴を上げない）
        double up = this.stateTimer < 16 ? 0.03 : 0.14;
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
        return seaSurfaceAt(this.level(), this.getBlockX(), this.getBlockZ());
    }

    /** (x, z) の一番上が水なら、その水のブロックの上面の高さ。水でなければ NaN。 */
    static double seaSurfaceAt(Level level, int x, int z) {
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (!level.getFluidState(new BlockPos(x, top - 1, z)).is(FluidTags.WATER)) {
            return Double.NaN;
        }
        return top;
    }

    /** 魚を食べさせてくれた人を覚える（{@link FishLureGoal}）。{@link #TRUST_TICKS}（5 分）で忘れる */
    void trust(Player player) {
        this.trustedPlayer = player.getUUID();
        this.trustTicks = TRUST_TICKS;
        // ⚠ 船に付いている最中に魚へ呼ばれると、付くのをやめた時の待ち（2〜4 分）が残り、食べた後に付いてこなかった
        //   （2026-10-07 に試した）。魚をくれた人の船へは、飛び立ったらすぐ付けるようにする
        this.followCooldown = 0;
    }

    /** この人を信用しているか（魚を食べさせてもらってから 5 分以内で、その後に殴られていない） */
    boolean trusts(Player player) {
        return this.trustTicks > 0 && player.getUUID().equals(this.trustedPlayer);
    }

    /** 人が近くても、しばらく水面から飛び立たない（魚を食べているところを見せる） */
    void calmFor(int ticks) {
        this.calmTicks = ticks;
    }

    /** 浮いている残りを ticks にする（魚をくれた人の船へすぐ付いていけるように、食べたら少しだけ浮いて飛び立つ） */
    void restBriefly(int ticks) {
        this.paddleTime = this.stateTimer + ticks;
    }

    /** 水面の決まった点へ急降下する（{@link FishLureGoal}。休みの降下 {@link #beginDive} と同じ動き） */
    void diveAt(double surface, float yaw) {
        this.surfaceY = surface;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        setState(DIVE);
        this.getNavigation().stop();
        if (this.moveControl instanceof ShearwaterMoveControl mc) {
            mc.halt();
        }
    }

    /** 波を切る間、滑空を保たせる（{@link ShearPassGoal}）。 */
    void holdGlide(int ticks) {
        this.glideHold = ticks;
        if (ticks > 0 && getState() == FLY) {
            setState(GLIDE);
        }
    }

    /** 水面に浮いたまま保つ。水が無ければ false。 */
    private boolean holdOnSurface() {
        double top = surfaceBelow();
        if (Double.isNaN(top)) {
            return false;
        }
        this.surfaceY = top;
        double want = top - 0.25;                      // 胴が水面に沈む分（軽く高く浮く）
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
        if (ok && this.level() instanceof ServerLevel sl && this.isAlive()) {
            triggerAnim("main", "hurt");
            // 嵐色の羽根が散る（タイヨウチョウは常に落とすが、こちらは殴られたときだけ）。
            // 体の羽 12 枚に風切羽 2 枚。散らす範囲は胴の大きさ（幅 1.5・長さ 6）に合わせる
            sl.sendParticles(ModParticles.FEATHER.get(), this.getX(), this.getY() + 1.0, this.getZ(),
                    12, 1.2, 0.5, 1.8, 0.03);
            sl.sendParticles(ModParticles.PLUME.get(), this.getX(), this.getY() + 1.2, this.getZ(),
                    2, 2.0, 0.4, 1.5, 0.02);
            int st = getState();
            if (st == PADDLE || st == SLEEP || st == DIVE) {
                beginTakeoff();
            }
            // 信用していた人に殴られたら忘れる（船にも付いてこなくなる）
            if (source.getEntity() instanceof Player p && trusts(p)) {
                this.trustTicks = 0;
                this.trustedPlayer = null;
            }
        }
        return ok;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel sl) {
            triggerAnim("main", "faint");
            sl.sendParticles(ModParticles.FEATHER.get(), this.getX(), this.getY() + 1.0, this.getZ(),
                    30, 2.5, 0.6, 2.5, 0.04);
            sl.sendParticles(ModParticles.PLUME.get(), this.getX(), this.getY() + 1.2, this.getZ(),
                    6, 4.0, 0.6, 2.5, 0.03);
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
        int bank = this.entityData.get(BANK);
        RawAnimation anim = switch (getState()) {
            case GLIDE -> bank < 0 ? ANIM_GLIDE_L : (bank > 0 ? ANIM_GLIDE_R : ANIM_GLIDE);
            case HOVER -> ANIM_HOVER;
            case DIVE, LAND -> ANIM_DIVE;
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

    /**
     * 海の上を途切れずに巡る。行き先の 5 ブロック手前で次の行き先を選ぶので、空中で止まらない
     * （前は着くたびに止まって 2〜6 秒待ち、そのたびに「止まる」の動きへ入っていた）。
     * 行き先は前方寄り（±63°）の 14〜32 ブロック先で、水の上を選ぶ。高さは水面の 2〜7 上、4 回に 1 回は 10〜22 上。
     * 着いたときに 8 回に 1 回だけ、その場で 2〜4 秒羽ばたいて止まる（餌を探す。平均 40 秒に 1 回ほど）。
     */
    static class SeaWanderGoal extends Goal {
        private final ShearwaterEntity bird;
        @Nullable
        private Vec3 target;
        private int legTime;
        private int hoverTime;
        /** 止まったあと 20 秒は止まらない（続けて止まると、行き先ごとに止まっていた前と同じに見える） */
        private int hoverCooldown = 400;
        private Vec3 hoverAt = Vec3.ZERO;

        SeaWanderGoal(ShearwaterEntity bird) {
            this.bird = bird;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            int st = this.bird.getState();
            return st == FLY || st == GLIDE || st == HOVER;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            this.target = null;
            this.hoverTime = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.hoverTime > 0) {
                this.bird.getMoveControl().setWantedPosition(this.hoverAt.x, this.hoverAt.y, this.hoverAt.z, 1.0);
                if (--this.hoverTime == 0) {
                    this.bird.setState(FLY);
                    this.target = null;
                }
                return;
            }
            this.legTime++;
            this.hoverCooldown--;
            boolean arrived = this.target != null && horizontalDistance(this.target) < 5.0;
            if (this.target == null || arrived || this.legTime > 300 || this.bird.horizontalCollision) {
                if (arrived && this.hoverCooldown <= 0 && this.bird.random.nextInt(8) == 0) {
                    this.hoverCooldown = 400;
                    this.hoverTime = 40 + this.bird.random.nextInt(41);
                    this.hoverAt = this.bird.position();
                    this.bird.setState(HOVER);
                    return;
                }
                this.target = pickTarget();
                this.legTime = 0;
            }
            if (this.target != null) {
                this.bird.getMoveControl().setWantedPosition(this.target.x, this.target.y, this.target.z, 1.0);
            }
        }

        private double horizontalDistance(Vec3 p) {
            double dx = p.x - this.bird.getX();
            double dz = p.z - this.bird.getZ();
            return Math.sqrt(dx * dx + dz * dz);
        }

        @Nullable
        private Vec3 pickTarget() {
            Level level = this.bird.level();
            RandomSource random = this.bird.random;
            float yaw = this.bird.getYRot() * Mth.DEG_TO_RAD;
            Vec3 eye = this.bird.getEyePosition();
            for (int i = 0; i < 12; i++) {
                // 前方寄り。見つからなければ後半は全方向から
                double ang = yaw + (random.nextDouble() - 0.5) * (i < 8 ? 2.2 : Math.PI * 2.0);
                double dist = 14.0 + random.nextDouble() * 18.0;
                double x = this.bird.getX() - Math.sin(ang) * dist;
                double z = this.bird.getZ() + Math.cos(ang) * dist;
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
                boolean water = level.getFluidState(new BlockPos(Mth.floor(x), top - 1, Mth.floor(z))).is(FluidTags.WATER);
                if (!water && i < 10) {
                    continue;
                }
                // 波すれすれ（2〜7 上）が基本。4 回に 1 回は舞い上がる（10〜22）
                double y = random.nextInt(4) == 0 ? top + 10 + random.nextInt(13) : top + 2 + random.nextInt(6);
                Vec3 to = new Vec3(x, y, z);
                if (level.clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.bird))
                        .getType() != HitResult.Type.MISS) {
                    continue;                           // 島や崖の向こうは選ばない
                }
                return to;
            }
            return null;
        }
    }
}
