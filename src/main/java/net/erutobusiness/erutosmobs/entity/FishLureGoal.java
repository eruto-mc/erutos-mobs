package net.erutobusiness.erutosmobs.entity;

import net.erutobusiness.erutosmobs.advancement.ShearwaterTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * 魚で寄せる（2026-09-29）。実物のミズナギドリは、船から捨てられた魚や、水面の小魚へ飛び込んで食べる。
 *
 * 水面に浮いた生の魚（鱈・鮭・熱帯魚。フグは食べない）が 32 ブロック以内にあると、上から近づき、
 * 3 ブロック手前から急降下して水面で食べる（品物を消す・食べる音・魚のかけら）。
 * 投げた人（品物の持ち主）を 5 分信用し、その人の船には付いて飛びやすくなる（{@link FollowBoatGoal}）。
 * 食べた後は 3 秒だけ、人が近くても浮いたまま（その後はいつもどおり、人が 6 ブロックに来たら飛び立つ）。
 * 近づく間に魚が消えたら（拾われた・消えた）やめる。食べてから 5 秒は次の魚を探さない。
 * 嵐の怒りを買っている人が投げた魚は取らない。
 */
class FishLureGoal extends Goal {
    private static final double SEARCH = 32.0;
    /** 急降下を始める水平の距離（降下は 1 tick に前 0.12・下 0.28 なので、6 ブロック上から約 2.6 前へ進んで水に入る） */
    private static final double DIVE_FROM = 3.0;

    private final ShearwaterEntity bird;
    @Nullable
    private ItemEntity fish;
    private boolean diving;
    private int time;
    private int searchDelay;
    private int cooldown;

    FishLureGoal(ShearwaterEntity bird) {
        this.bird = bird;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    private static boolean isFish(ItemStack stack) {
        return stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.TROPICAL_FISH);
    }

    /** 水に浮いているか（品物のいる所が水か、すぐ下が水面） */
    private static boolean floating(ItemEntity e) {
        Level level = e.level();
        BlockPos p = e.blockPosition();
        return e.isInWater() || level.getFluidState(p).is(FluidTags.WATER) || level.getFluidState(p.below()).is(FluidTags.WATER);
    }

    private static boolean flying(int st) {
        return st == ShearwaterEntity.FLY || st == ShearwaterEntity.GLIDE || st == ShearwaterEntity.HOVER;
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        if (--this.searchDelay > 0) {
            return false;
        }
        this.searchDelay = 10;
        if (!flying(this.bird.getState())) {
            return false;
        }
        List<ItemEntity> found = this.bird.level().getEntitiesOfClass(ItemEntity.class,
                this.bird.getBoundingBox().inflate(SEARCH, 24.0, SEARCH),
                e -> e.isAlive() && isFish(e.getItem()) && floating(e) && !ShearwaterEntity.wrathful(e.getOwner()));
        ItemEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (ItemEntity e : found) {
            double d = e.distanceToSqr(this.bird);
            if (d < bestD) {
                best = e;
                bestD = d;
            }
        }
        this.fish = best;
        return best != null;
    }

    @Override
    public void start() {
        this.diving = false;
        this.time = 0;
        if (this.bird.getState() == ShearwaterEntity.HOVER) {
            this.bird.setState(ShearwaterEntity.FLY);
        }
        this.bird.getNavigation().stop();
    }

    @Override
    public boolean canContinueToUse() {
        ItemEntity f = this.fish;
        if (f == null || !f.isAlive() || this.time > 400) {
            return false;
        }
        int st = this.bird.getState();
        return this.diving ? (st == ShearwaterEntity.DIVE || st == ShearwaterEntity.PADDLE) : flying(st);
    }

    @Override
    public void stop() {
        this.fish = null;
        this.diving = false;
        if (this.cooldown == 0) {
            this.cooldown = 40;
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ItemEntity f = this.fish;
        if (f == null) {
            return;
        }
        this.time++;
        double surface = ShearwaterEntity.seaSurfaceAt(this.bird.level(), f.getBlockX(), f.getBlockZ());
        if (Double.isNaN(surface)) {
            this.fish = null;
            return;
        }
        double dx = f.getX() - this.bird.getX();
        double dz = f.getZ() - this.bird.getZ();
        double h = Math.sqrt(dx * dx + dz * dz);
        float yawToFish = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
        if (this.diving) {
            if (this.bird.getState() == ShearwaterEntity.DIVE) {
                // 降りる間も魚の方へ向き直る（降下は向いた方へ進む）
                this.bird.setYRot(yawToFish);
                this.bird.yBodyRot = yawToFish;
                return;
            }
            // 水に入った（浮いた）: 近くに魚があれば食べる
            if (h < 3.5) {
                eat(f);
            }
            this.fish = null;
            return;
        }
        if (h < DIVE_FROM && this.bird.getY() > surface + 2.5) {
            this.diving = true;
            this.bird.diveAt(surface, yawToFish);
            return;
        }
        // 近づく: 魚の手前 DIVE_FROM の点の 6 上（遠ければ 8 上）へ
        double k = h > 0.1 ? Math.max(0.0, h - DIVE_FROM + 0.5) / h : 0.0;
        double tx = this.bird.getX() + dx * k;
        double tz = this.bird.getZ() + dz * k;
        double ty = surface + (h < 12.0 ? 6.0 : 8.0);
        this.bird.getMoveControl().setWantedPosition(tx, ty, tz, h < 12.0 ? 0.8 : 1.0);
    }

    private void eat(ItemEntity f) {
        Level level = this.bird.level();
        ItemStack stack = f.getItem().copy();
        Entity owner = f.getOwner();
        f.discard();
        level.playSound(null, this.bird.getX(), this.bird.getY(), this.bird.getZ(), SoundEvents.GENERIC_EAT,
                SoundSource.NEUTRAL, 1.2f, 0.7f + this.bird.getRandom().nextFloat() * 0.2f);
        if (level instanceof ServerLevel sl) {
            Vec3 head = this.bird.position().add(Vec3.directionFromRotation(0.0f, this.bird.getYRot()).scale(2.0)).add(0.0, 1.0, 0.0);
            sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack), head.x, head.y, head.z, 12, 0.3, 0.2, 0.3, 0.05);
        }
        if (owner instanceof Player p) {
            this.bird.trust(p);
            if (p instanceof ServerPlayer sp) {
                ShearwaterTrigger.INSTANCE.trigger(sp, ShearwaterTrigger.FED);
            }
            // ⚠ 食べた後にいつもどおり 15〜45 秒浮いていると、くれた人が船で行ってしまってから飛び立つ（2026-10-07 に試して、
            //   付いて飛ぶのが 30 秒遅れた）。くれた人がいるときは 3〜4 秒だけ浮いて飛び立つ
            this.bird.restBriefly(60 + this.bird.getRandom().nextInt(21));
        }
        this.bird.calmFor(60);
        this.cooldown = 100;
    }
}
