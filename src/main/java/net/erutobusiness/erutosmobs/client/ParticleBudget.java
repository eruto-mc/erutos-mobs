package net.erutobusiness.erutosmobs.client;

import net.erutobusiness.erutosmobs.ErutosMobsClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;

/**
 * ミズナギドリの粒子をどれだけ出すか（0〜1）。各自の設定 `particleAmount` に、ゲームの「パーティクル」の設定を掛ける
 * （すべて 1・少なめ 0.5・最小 0.25）。
 * ⚠ 当 MOD の粒子の多くは遠くでも出す型（`SimpleParticleType(true)`）で、ゲームの設定では減らない
 *   （`LevelRenderer.addParticleInternal` は、遠くでも出す型には「最小」の間引きを当てない）。だからここで間引く。
 * ⚠ 画面の側のクラス（Minecraft）を使うので、鳥の側（{@code ShearwaterEffects}）には書かず、ここから呼ぶ。
 */
public final class ParticleBudget {
    public static float amount() {
        float a = ErutosMobsClientConfig.PARTICLE_AMOUNT.get().floatValue();
        ParticleStatus s = Minecraft.getInstance().options.particles().get();
        return a * (s == ParticleStatus.ALL ? 1.0f : s == ParticleStatus.DECREASED ? 0.5f : 0.25f);
    }

    private ParticleBudget() {
    }
}
