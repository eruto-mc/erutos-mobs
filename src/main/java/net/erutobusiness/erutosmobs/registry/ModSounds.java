package net.erutobusiness.erutosmobs.registry;

import net.erutobusiness.erutosmobs.ErutosMobs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 音の口。ogg は `assets/erutosmobs/sounds/` に置く（手元の音の道具 wavs の `make.py --ship` が作って写す）。 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ErutosMobs.MOD_ID);

    public static final RegistryObject<SoundEvent> SHEARWATER_CRY = register("shearwater_cry");
    public static final RegistryObject<SoundEvent> SHEARWATER_HURT = register("shearwater_hurt");
    public static final RegistryObject<SoundEvent> SHEARWATER_DEATH = register("shearwater_death");
    public static final RegistryObject<SoundEvent> SHEARWATER_FLAP = register("shearwater_flap");
    /** 近くを飛び過ぎる風切り（画面の側で、通り過ぎる少し前に鳴らす） */
    public static final RegistryObject<SoundEvent> SHEARWATER_FLYBY = register("shearwater_flyby");
    /** 翼端が波を切る水の音 */
    public static final RegistryObject<SoundEvent> SHEARWATER_SHEAR = register("shearwater_shear");
    /** 帯電のパチパチ */
    public static final RegistryObject<SoundEvent> SHEARWATER_CRACKLE = register("shearwater_crackle");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ErutosMobs.MOD_ID, name)));
    }

    private ModSounds() {
    }
}
