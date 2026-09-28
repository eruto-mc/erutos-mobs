package net.erutobusiness.erutosmobs.entity;

/**
 * 粒子の出どころ（ブロック単位・実体の足元が原点・yaw 0＝南向きのとき。+X が鳥の左、+Z が前）。
 * ⚠ 自動生成。mc-model-kit の `entities/shearwater/ship_to_mod.py` が書き出した模型から計算して書く。手で直さない。
 * yaw θ の世界へは {@link #toWorld} を使う。
 */
public final class ShearwaterLocators {
    public static final double[] GLIDE_TIP_L = {6.136, 2.354, 0.453};
    public static final double[] GLIDE_TIP_R = {-6.082, 2.654, -0.188};
    public static final double[] BANK_L_TIP_L = {6.181, 0.053, -0.276};
    public static final double[] BANK_L_TIP_R = {-4.586, 5.400, 0.354};
    public static final double[] BANK_R_TIP_L = {5.202, 4.459, 1.049};
    public static final double[] BANK_R_TIP_R = {-6.034, -0.409, 0.237};
    public static final double[] FOOT_L = {0.202, 0.052, 0.480};
    public static final double[] FOOT_R = {-0.203, 0.052, 0.480};
    public static final double[] BODY_CENTER = {0.012, 0.975, -0.300};
    public static final double[] BODY_BOTTOM_Y = {0.667, 0.000, 0.000};
    public static final double[][] TRAILING_EDGE = {{0.722, 1.310, -0.511}, {1.447, 1.381, -0.448}, {2.523, 1.569, -0.181}, {3.925, 1.877, 0.293}, {5.326, 2.185, 0.767}, {-0.655, 1.344, -0.583}, {-1.378, 1.451, -0.596}, {-2.465, 1.692, -0.443}, {-3.892, 2.069, -0.117}, {-5.319, 2.447, 0.209}};

    /** yaw 0 の点を、体の向き yawDeg（Minecraft の yBodyRot）の世界の差分へ回す。 */
    public static double[] toWorld(double[] p, float yawDeg) {
        double t = Math.toRadians(yawDeg);
        double c = Math.cos(t), s = Math.sin(t);
        return new double[] {p[0] * c - p[2] * s, p[1], p[0] * s + p[2] * c};
    }

    private ShearwaterLocators() {
    }
}
