package net.erutobusiness.erutosmobs.client;

import net.erutobusiness.erutosmobs.ErutosMobs;
import net.erutobusiness.erutosmobs.registry.ModEntities;
import net.erutobusiness.erutosmobs.registry.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ErutosMobs.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SHEARWATER.get(), ShearwaterRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.FEATHER.get(), FeatherParticle.Provider::new);
        event.registerSpriteSet(ModParticles.PLUME.get(), FeatherParticle.PlumeProvider::new);
        event.registerSpriteSet(ModParticles.MIST.get(), MistParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SPRAY.get(), SprayParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SPARK.get(), SparkParticle.Provider::new);
    }

    private ClientEvents() {
    }
}
