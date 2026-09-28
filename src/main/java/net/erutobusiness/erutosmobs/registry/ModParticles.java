package net.erutobusiness.erutosmobs.registry;

import net.erutobusiness.erutosmobs.ErutosMobs;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 当 MOD の粒子。絵と動きはクライアントの {@code client.FeatherParticle} / {@code client.MistParticle}。
 *
 * 羽根はタイヨウチョウ（Alex's Mobs）の `sunbird_feather`（8×8 の 2 枚）と同じ作り。
 * こちらは常に落とさず、殴られたときだけ散らす（嵐の鳥は軌跡を羽根ではなく風で残す）。
 */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, ErutosMobs.MOD_ID);

    /** 嵐色の羽根（殴られたとき）。絵はキットが塗る `textures/particle/shearwater_feather_{0,1}.png` */
    public static final RegistryObject<SimpleParticleType> FEATHER =
            PARTICLES.register("shearwater_feather", () -> new SimpleParticleType(false));
    /** 翼端の風の筋（滑空中）。絵は Minecraft の雲の粒の絵（generic）を小さく淡く使う */
    public static final RegistryObject<SimpleParticleType> MIST =
            PARTICLES.register("shearwater_mist", () -> new SimpleParticleType(false));

    private ModParticles() {
    }
}
