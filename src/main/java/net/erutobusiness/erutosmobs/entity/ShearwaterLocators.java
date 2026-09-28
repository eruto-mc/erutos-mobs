package net.erutobusiness.erutosmobs.entity;

/**
 * 粒子の出どころ（ブロック単位・実体の足元が原点・yaw 0＝南向きのとき。+X が鳥の左、+Z が前）。
 * ⚠ 自動生成。mc-model-kit の `entities/shearwater/ship_to_mod.py` が書き出した模型から計算して書く。手で直さない。
 * yaw θ の世界へは {@link #toWorld} を使う。
 */
public final class ShearwaterLocators {
    public static final double[] GLIDE_TIP_L = {5.901, 2.222, 0.152};
    public static final double[] GLIDE_TIP_R = {-5.824, 2.511, -0.463};
    public static final double[] BANK_L_TIP_L = {5.882, 0.014, -0.543};
    public static final double[] BANK_L_TIP_R = {-4.462, 5.151, 0.062};
    public static final double[] BANK_R_TIP_L = {5.065, 4.252, 0.731};
    public static final double[] BANK_R_TIP_R = {-5.679, -0.440, -1.126};
    public static final double[] FOOT_L = {0.202, 0.052, 0.480};
    public static final double[] FOOT_R = {-0.203, 0.052, 0.480};
    public static final double[] BODY_CENTER = {0.012, 0.975, -0.300};
    public static final double[] BODY_BOTTOM_Y = {0.667, 0.000, 0.000};
    public static final double[][] TRAILING_EDGE = {{0.731, 1.330, -0.397}, {1.094, 1.365, -0.441}, {1.451, 1.400, -0.410}, {2.353, 1.536, -0.271}, {2.988, 1.650, -0.203}, {3.728, 1.787, -0.104}, {4.916, 2.019, -0.070}, {-0.675, 1.365, -0.471}, {-1.029, 1.418, -0.552}, {-1.385, 1.470, -0.558}, {-2.289, 1.650, -0.514}, {-2.921, 1.795, -0.513}, {-3.660, 1.969, -0.491}, {-4.832, 2.259, -0.581}};

    /** yaw 0 の点を、体の向き yawDeg（Minecraft の yBodyRot）の世界の差分へ回す。 */
    public static double[] toWorld(double[] p, float yawDeg) {
        double t = Math.toRadians(yawDeg);
        double c = Math.cos(t), s = Math.sin(t);
        return new double[] {p[0] * c - p[2] * s, p[1], p[0] * s + p[2] * c};
    }

    private ShearwaterLocators() {
    }
}
