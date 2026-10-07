package net.erutobusiness.erutosmobs.client;

import net.erutobusiness.erutosmobs.entity.ShearwaterEntity;
import net.erutobusiness.erutosmobs.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 画面の側だけで鳴らす音のうち、この画面の人（聞き手）を見て決める物。
 * ⚠ 画面の側のクラス（Minecraft）を使うので、鳥の側（{@code ShearwaterEffects}、サーバにも在るクラス）には書かず、ここから呼ぶ。
 */
public final class ShearwaterSounds {
    /** 近づく速さがこれより遅ければ鳴らさない（ブロック/tick） */
    private static final double MIN_SPEED = 0.2;
    /** いちばん近づく所がこれより遠ければ鳴らさない */
    private static final double PASS_WITHIN = 10.0;

    /**
     * 飛び過ぎる風切りを鳴らす。音の山は鳴らし始めの 0.55 秒後なので、いちばん近づく 6〜14 tick 前に鳴らす。
     * 音は鳥について動く（{@link EntityBoundSoundInstance}）。鳴らしたら true
     */
    public static boolean flyby(ShearwaterEntity bird) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null) {
            return false;
        }
        Vec3 v = new Vec3(bird.getX() - bird.xo, bird.getY() - bird.yo, bird.getZ() - bird.zo);
        double vv = v.lengthSqr();
        if (vv < MIN_SPEED * MIN_SPEED) {
            return false;
        }
        Vec3 rel = bird.position().add(0.0, 1.0, 0.0).subtract(p.getEyePosition());
        double ticks = -rel.dot(v) / vv;
        if (ticks < 6.0 || ticks > 14.0 || rel.add(v.scale(ticks)).length() > PASS_WITHIN) {
            return false;
        }
        mc.getSoundManager().play(new EntityBoundSoundInstance(ModSounds.SHEARWATER_FLYBY.get(), SoundSource.NEUTRAL,
                1.2f, 0.9f + bird.getRandom().nextFloat() * 0.2f, bird, bird.getRandom().nextLong()));
        return true;
    }

    private ShearwaterSounds() {
    }
}
