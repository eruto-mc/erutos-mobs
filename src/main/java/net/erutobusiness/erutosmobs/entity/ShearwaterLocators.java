package net.erutobusiness.erutosmobs.entity;

/**
 * 粒子の出どころ（ブロック単位・実体の足元が原点・yaw 0＝南向きのとき。+X が鳥の左、+Z が前）。
 * ⚠ 自動生成。mc-model-kit の `entities/shearwater/ship_to_mod.py` が書き出した模型から計算して書く。手で直さない。
 * yaw θ の世界へは {@link #toWorld} を使う。
 */
public final class ShearwaterLocators {
    public static final double[] GLIDE_TIP_L = {5.883, 2.310, 0.151};
    public static final double[] GLIDE_TIP_R = {-5.801, 2.598, -0.462};
    public static final double[] BANK_L_TIP_L = {5.895, 0.102, -0.546};
    public static final double[] BANK_L_TIP_R = {-4.400, 5.215, 0.056};
    public static final double[] BANK_R_TIP_L = {5.017, 4.328, 0.724};
    public static final double[] BANK_R_TIP_R = {-5.700, -0.352, -1.128};
    public static final double[] FOOT_L = {0.202, 0.056, 0.493};
    public static final double[] FOOT_R = {-0.203, 0.056, 0.493};
    public static final double[] BODY_CENTER = {0.012, 0.975, -0.300};
    public static final double[] BODY_BOTTOM_Y = {0.667, 0.000, 0.000};

    /**
     * 描いた骨から取る点。`ShearwaterRenderer` が、描いたときの骨の行列（GeckoLib の `getLocalSpaceMatrix`）に
     * {@link #LIVE_OFFSETS} を掛けて毎コマ出す（実体から見た位置・世界の向き）。描かれていない間は
     * {@link #LIVE_GLIDE}（滑空の姿勢・yaw 0）を {@link #toWorld} で回して代わりにする。
     * 並び: 翼端 左・右 → 後縁 左・右（付け根から先へ）→ 前縁 左・右（肩・手首・腕の先）
     */
    public static final String[] LIVE_BONES = {"prim8_l", "prim8_r", "wing_l", "wing_l", "wing_l", "prim1_l", "prim3_l", "prim5_l", "prim7_l", "wing_r", "wing_r", "wing_r", "prim1_r", "prim3_r", "prim5_r", "prim7_r", "wing_l", "wing_out_l", "wing_out_l", "wing_r", "wing_out_r", "wing_out_r"};
    /** 骨の回転の中心から見た点（GeckoLib の中の座標・ブロック単位） */
    public static final float[][] LIVE_OFFSETS = {{0.0000f, 0.0000f, 3.7500f}, {0.0000f, 0.0000f, 3.7500f}, {-0.7063f, 0.0063f, 1.1250f}, {-1.3062f, 0.0063f, 1.2500f}, {-1.9062f, 0.0063f, 1.2500f}, {0.0000f, 0.0000f, 1.6250f}, {0.0000f, 0.0000f, 1.7500f}, {0.0000f, 0.0000f, 1.9375f}, {0.0000f, 0.0000f, 2.8125f}, {0.7063f, 0.0063f, 1.1250f}, {1.3062f, 0.0063f, 1.2500f}, {1.9062f, 0.0063f, 1.2500f}, {0.0000f, 0.0000f, 1.6250f}, {0.0000f, 0.0000f, 1.7500f}, {0.0000f, 0.0000f, 1.9375f}, {0.0000f, 0.0000f, 2.8125f}, {0.0000f, 0.0000f, 0.0000f}, {0.0000f, 0.0000f, 0.0000f}, {-4.4375f, 0.0312f, -0.5156f}, {0.0000f, 0.0000f, 0.0000f}, {0.0000f, 0.0000f, 0.0000f}, {4.4375f, 0.0312f, -0.5156f}};
    public static final double[][] LIVE_GLIDE = {{5.883, 2.310, 0.151}, {-5.801, 2.598, -0.462}, {0.732, 1.315, -0.397}, {1.096, 1.350, -0.441}, {1.453, 1.386, -0.410}, {2.357, 1.517, -0.271}, {2.987, 1.653, -0.203}, {3.722, 1.813, -0.105}, {4.906, 2.067, -0.071}, {-0.677, 1.350, -0.471}, {-1.031, 1.403, -0.552}, {-1.388, 1.455, -0.558}, {-2.294, 1.632, -0.515}, {-2.920, 1.799, -0.513}, {-3.653, 1.994, -0.491}, {-4.820, 2.306, -0.580}, {0.254, 1.269, 0.238}, {1.741, 1.415, 0.369}, {4.230, 1.981, 1.187}, {-0.270, 1.282, 0.211}, {-1.754, 1.501, 0.185}, {-4.284, 2.191, 0.741}};
    /** TIP の最初の番号（左 1・右 1） */
    public static final int TIP = 0;
    public static final int TIP_PER_SIDE = 1;
    /** EDGE の最初の番号（左 7・右 7） */
    public static final int EDGE = 2;
    public static final int EDGE_PER_SIDE = 7;
    /** ARM の最初の番号（左 3・右 3） */
    public static final int ARM = 16;
    public static final int ARM_PER_SIDE = 3;

    /** yaw 0 の点を、体の向き yawDeg（Minecraft の yBodyRot）の世界の差分へ回す。 */
    public static double[] toWorld(double[] p, float yawDeg) {
        double t = Math.toRadians(yawDeg);
        double c = Math.cos(t), s = Math.sin(t);
        return new double[] {p[0] * c - p[2] * s, p[1], p[0] * s + p[2] * c};
    }

    private ShearwaterLocators() {
    }
}
