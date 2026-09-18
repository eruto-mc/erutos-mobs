package net.erutobusiness.erutosmobs.registry;

import net.erutobusiness.erutosmobs.ErutosMobs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 音の口。ogg は `assets/erutosmobs/sounds/` に置く（wavs で作る予定。無いあいだは警告が出るだけ）。 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ErutosMobs.MOD_ID);

    public static final RegistryObject<SoundEvent> SHEARWATER_CRY = register("shearwater_cry");
    public static final RegistryObject<SoundEvent> SHEARWATER_HURT = register("shearwater_hurt");
    public static final RegistryObject<SoundEvent> SHEARWATER_DEATH = register("shearwater_death");
    public static final RegistryObject<SoundEvent> SHEARWATER_FLAP = register("shearwater_flap");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ErutosMobs.MOD_ID, name)));
    }

    private ModSounds() {
    }
}
