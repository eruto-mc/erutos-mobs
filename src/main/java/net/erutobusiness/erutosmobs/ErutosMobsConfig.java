package net.erutobusiness.erutosmobs;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * サーバ側の設定（`config/erutosmobs-common.toml`）。
 * Alex's Mobs が湧きの出やすさを設定で持つのに倣い、伝説の稀さを運用で変えられるようにする。
 */
public final class ErutosMobsConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue SHEARWATER_SPAWN_ONE_IN;
    public static final ForgeConfigSpec.IntValue SHEARWATER_MIN_DISTANCE;
    public static final ForgeConfigSpec.IntValue SHEARWATER_DESPAWN_DISTANCE;
    public static final ForgeConfigSpec.IntValue SHEARWATER_STORM_LIGHTNING_ONE_IN;
    public static final ForgeConfigSpec.IntValue SHEARWATER_STORM_SPAWN_BOOST;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("shearwater");
        SHEARWATER_SPAWN_ONE_IN = b
                .comment("Natural spawn passes only one time in N (on top of the biome weight). 1 = every time.")
                .defineInRange("spawnOneIn", 25, 1, 100000);
        SHEARWATER_MIN_DISTANCE = b
                .comment("No natural spawn if another shearwater is within this many blocks.")
                .defineInRange("minDistanceBetween", 192, 0, 4096);
        SHEARWATER_DESPAWN_DISTANCE = b
                .comment("Despawns only when every player is farther than this (blocks). Vanilla water creatures use 128.")
                .defineInRange("despawnDistance", 256, 64, 1024);
        SHEARWATER_STORM_LIGHTNING_ONE_IN = b
                .comment("While flying in a thunderstorm, calls a visual-only lightning bolt onto itself one tick in N",
                        "(no fire, no damage; it becomes charged for a minute). 1800 = about every 90 s. 0 = never.")
                .defineInRange("stormLightningOneIn", 1800, 0, 1000000);
        SHEARWATER_STORM_SPAWN_BOOST = b
                .comment("During a thunderstorm, spawnOneIn is divided by this (5 = five times as likely). 1 = no change.",
                        "The minimum distance between shearwaters still applies.")
                .defineInRange("stormSpawnBoost", 5, 1, 1000);
        b.pop();
        SPEC = b.build();
    }

    private ErutosMobsConfig() {
    }
}
