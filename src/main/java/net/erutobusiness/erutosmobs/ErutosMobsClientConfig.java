package net.erutobusiness.erutosmobs;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 各自の画面側の設定（`config/erutosmobs-client.toml`）。人ごとに変えてよい物だけを置く。
 */
public final class ErutosMobsClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_AMOUNT;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("shearwater");
        PARTICLE_AMOUNT = b
                .comment("How many of the shearwater's particles to show (spray, foam, wind trails, sparks, drips).",
                        "1.0 = all, 0.5 = half, 0 = none. Multiplied again by the game's own Particles setting",
                        "(All 1.0, Decreased 0.5, Minimal 0.25). Lightning on the wings is kept or dropped one bolt at a time.")
                .defineInRange("particleAmount", 1.0, 0.0, 1.0);
        b.pop();
        SPEC = b.build();
    }

    private ErutosMobsClientConfig() {
    }
}
