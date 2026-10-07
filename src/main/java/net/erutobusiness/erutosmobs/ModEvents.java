package net.erutobusiness.erutosmobs;

import net.erutobusiness.erutosmobs.registry.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** ゲーム中の出来事（Forge の側の event bus）。 */
@Mod.EventBusSubscriber(modid = ErutosMobs.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ModEvents {
    /** 嵐渡りの間は雷に打たれない（傷も火も無い）。`LightningBolt.tick` はこれが取り消されると `thunderHit` を呼ばない */
    @SubscribeEvent
    public static void onLightning(EntityStruckByLightningEvent event) {
        if (event.getEntity() instanceof LivingEntity living && living.hasEffect(ModEffects.STORM_CROSSING.get())) {
            event.setCanceled(true);
        }
    }

    private ModEvents() {
    }
}
