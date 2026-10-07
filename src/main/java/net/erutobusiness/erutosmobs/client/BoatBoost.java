package net.erutobusiness.erutosmobs.client;

import net.erutobusiness.erutosmobs.ErutosMobs;
import net.erutobusiness.erutosmobs.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 嵐渡りの間、水の上で前へ漕ぐと船が速い。
 * ⚠ 人が漕ぐ船は、漕ぐ人の画面の側が動かしてサーバへ位置を送る（`Boat.isControlledByLocalInstance`）。
 *   だからサーバでは足せず、ここで足す。
 * 1 tick に 0.01 を前へ足す。水の上の船は毎 tick 速さが 0.9 倍になる（`Boat.floatBoat`）ので、
 * 速さの上限は 0.01 × 0.9 ÷ 0.1 ＝ 0.09 上がる（ふだんの上限 約 0.4 の 2 割ほど）。氷の上では足さない。
 * 2026-10-08 に開発用のクライアントで漕いで測った: 嵐渡りの間 0.49、無いとき 0.40 ブロック/tick（中央値）。
 */
@Mod.EventBusSubscriber(modid = ErutosMobs.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BoatBoost {
    private static final double PUSH = 0.01;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || !(p.getVehicle() instanceof Boat boat) || !boat.isControlledByLocalInstance()
                || !p.input.up || !p.hasEffect(ModEffects.STORM_CROSSING.get())) {
            return;
        }
        if (!boat.level().getFluidState(boat.blockPosition()).is(FluidTags.WATER)
                && !boat.level().getFluidState(boat.blockPosition().below()).is(FluidTags.WATER)) {
            return;
        }
        Vec3 fwd = Vec3.directionFromRotation(0.0f, boat.getYRot());
        boat.setDeltaMovement(boat.getDeltaMovement().add(fwd.scale(PUSH)));
    }

    private BoatBoost() {
    }
}
