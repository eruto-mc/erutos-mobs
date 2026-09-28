package net.erutobusiness.erutosmobs.registry;

import net.erutobusiness.erutosmobs.ErutosMobs;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 当 MOD の粒子。絵と動きはクライアントの {@code client.*Particle}。
 *
 * ⚠⚠ 大きさの物差し（2026-09-28・ユーザー「パーティクルが体に対して小さすぎる」）:
 *   ミズナギドリは翼幅 12 ブロックで、実物（翼幅 1.1 m）の約 11 倍。粒子も「実物の大きさ × 11」で決める
 *   （1 ブロック ＝ 1 m）。⚠ バニラの粒子は幅 0.2〜0.4 ブロック（`SingleQuadParticle.quadSize` の既定）で、
 *   背丈 1.8 の人に合わせた大きさ。そのまま使うと翼幅の 2〜3% の粉にしかならなかった。
 *   だからバニラの絵を使う粒子も、大きさを自分で持つ型にして出す。
 */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, ErutosMobs.MOD_ID);

    /** 体の羽（実物 4〜6 cm → 幅 0.5〜0.8）。殴られたとき・死んだとき。絵はキットが塗る `shearwater_feather_{0,1}` */
    public static final RegistryObject<SimpleParticleType> FEATHER =
            PARTICLES.register("shearwater_feather", () -> new SimpleParticleType(false));
    /** 風切羽（実物 15〜20 cm → 幅 1.6〜2.2）。殴られたとき・死んだときに少しだけ。絵は `shearwater_plume_{0,1}`（16×16） */
    public static final RegistryObject<SimpleParticleType> PLUME =
            PARTICLES.register("shearwater_plume", () -> new SimpleParticleType(false));
    /**
     * 翼端の風の筋（滑空中）。絵は Minecraft の煙の粒（generic）を淡く太く使う。
     * ⚠ 引数 true＝32 ブロックより遠くても出す（バニラは遠い粒子を出さない。翼幅 12 の鳥は遠くから見える）。
     *   しぶき・火花も同じ
     */
    public static final RegistryObject<SimpleParticleType> MIST =
            PARTICLES.register("shearwater_mist", () -> new SimpleParticleType(true));
    /**
     * 大きなしぶき（絵の幅 0.6〜1.1。大きな粒はその半分ほど＝実物 3〜5 cm × 11）。絵はキットが描く水の粒 `shearwater_spray_{0..3}`。
     * ⚠ バニラのしぶき（splash_0〜3）は 8×8 の隅の 2〜3 画素で、海の上では見えなかった（2026-09-28）
     */
    public static final RegistryObject<SimpleParticleType> SPRAY =
            PARTICLES.register("shearwater_spray", () -> new SimpleParticleType(true));
    /** 水面の泡（幅 0.8〜1.4。水面に寝かせる）。波を切った線・航跡・着水の輪。絵はキットが描く `shearwater_foam_{0,1}` */
    public static final RegistryObject<SimpleParticleType> FOAM =
            PARTICLES.register("shearwater_foam", () -> new SimpleParticleType(true));
    /**
     * 稲妻の折れ目の光の点（幅 0.28〜0.4。暗くても明るい）。絵はキットが描く光の輪 `shearwater_spark_{0,1}`。
     * ⚠ バニラの光の点（glow）は太さ 1 画素の十字で、見えなかった（2026-09-28）
     */
    public static final RegistryObject<SimpleParticleType> SPARK =
            PARTICLES.register("shearwater_spark", () -> new SimpleParticleType(true));
    /**
     * 稲妻のひと区切り（幅 0.2〜0.26 の帯。出した点から、速さの欄に渡した向きと長さの先まで）。絵は `shearwater_bolt_0`。
     * ⚠ 光の点を並べた稲妻は「数珠」に見えた（2026-09-28）
     */
    public static final RegistryObject<SimpleParticleType> BOLT =
            PARTICLES.register("shearwater_bolt", () -> new SimpleParticleType(true));

    private ModParticles() {
    }
}
