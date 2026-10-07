package net.erutobusiness.erutosmobs.effect;

import net.erutobusiness.erutosmobs.entity.ShearwaterEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 嵐の怒り。ミズナギドリを傷つけた人に付く（殴った・射た 5 分、倒した 20 分）。
 *   ・雷雨の間は平均 15 秒に 1 回、ミズナギドリの 48 ブロック以内では晴れていても平均 20 秒に 1 回、
 *     4〜9 ブロック先に見た目だけの雷が落ちる（火も傷も無い。`LightningBolt.setVisualOnly`）
 *   ・ミズナギドリはその人の船に付かず、その人が投げた魚も取らない
 * 牛乳では消えない（嵐の怒りは時が過ぎるまで解けない）。
 */
public class StormWrathEffect extends MobEffect {
    private static final int STORM_ONE_IN = 300;
    private static final int NEAR_BIRD_ONE_IN = 400;
    private static final double NEAR_BIRD = 48.0;

    public StormWrathEffect() {
        super(MobEffectCategory.HARMFUL, 0x3E4662);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        RandomSource r = entity.getRandom();
        boolean storm = level.isThundering();
        if (r.nextInt(storm ? STORM_ONE_IN : NEAR_BIRD_ONE_IN) != 0) {
            return;
        }
        if (!storm && level.getEntitiesOfClass(ShearwaterEntity.class, entity.getBoundingBox().inflate(NEAR_BIRD)).isEmpty()) {
            return;
        }
        double a = r.nextDouble() * Math.PI * 2.0;
        double d = 4.0 + r.nextDouble() * 5.0;
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,
                BlockPos.containing(entity.getX() + Math.cos(a) * d, 0.0, entity.getZ() + Math.sin(a) * d));
        if (!level.canSeeSky(top)) {
            return;
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(Vec3.atBottomCenterOf(top));
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return new ArrayList<>();
    }

    /** 怒りの長さ（tick）。倒したら 20 分、傷つけたら 5 分 */
    public static int durationFor(boolean killed) {
        return killed ? 24000 : 6000;
    }
}
