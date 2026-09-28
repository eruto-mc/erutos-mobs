package net.erutobusiness.erutosmobs.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * 飛び方の制御。向き・速さ・上下の速さを自分で持ち、毎 tick 速度をそのまま入れる。
 *
 * 前はバニラの FlyingMoveControl（ハチ・オウムの物）だった。向きを 1 tick で 90° まで変え、上下は
 * 「上か下かへ全力」しか出さないので、この大きさの鳥では、行き先に着くたびに止まる・上下に大きく揺れる・
 * 水に潜るが出た（2026-09-28 に開発サーバで 255 秒測った: 空中で止まる 36%・滑空 6%・y は水面の 2 下から 35 上まで）。
 * タイヨウチョウ（Alex's Mobs 1.22.9 の `EntitySunbird$MoveHelperController`）は、行き先へ向けて速度に足し込み、
 * 向きは速度の向きに合わせる。こちらは曲がる半径を持たせたいので、向きの変わる速さに上限を置く
 * （速さ ÷ 6 ブロック。0.27 ブロック/tick なら 1 tick 2.6°、半回りに約 3.5 秒）。
 *
 * 速度を直に入れる状態（降りる・離陸・水面・浜）では何もしない。そのときの速さを覚えておき、
 * 飛ぶ状態へ戻ったときにそこから続ける。
 */
public class ShearwaterMoveControl extends MoveControl {
    /** 羽ばたいて進む速さ（ブロック/tick） */
    static final double CRUISE_FLY = 0.22;
    /** 滑空の速さ。高さを速さに換えて進むので羽ばたきより速い */
    static final double CRUISE_GLIDE = 0.27;
    private static final double MIN_TURN_RADIUS = 6.0;
    /** 地面・水面からこれより下へは、行き先に関わらず降りない（波を切る間と浜へ降りるときは除く） */
    private static final double FLOOR = 1.5;

    private final ShearwaterEntity bird;
    private double speed;
    private double vy;

    public ShearwaterMoveControl(ShearwaterEntity bird) {
        super(bird);
        this.bird = bird;
    }

    /** 行き先を捨てる（速度を直に入れる動きへ入るとき）。 */
    public void halt() {
        this.operation = Operation.WAIT;
    }

    @Override
    public void tick() {
        this.mob.setXxa(0.0f);
        this.mob.setYya(0.0f);
        this.mob.setZza(0.0f);
        int st = this.bird.getState();
        boolean flying = st == ShearwaterEntity.FLY || st == ShearwaterEntity.GLIDE
                || st == ShearwaterEntity.HOVER || st == ShearwaterEntity.LAND;
        if (!flying || this.operation != Operation.MOVE_TO) {
            // 速度はほかが入れている。今の速さを覚えておくだけ
            Vec3 v = this.bird.getDeltaMovement();
            this.speed = v.horizontalDistance();
            this.vy = v.y;
            return;
        }
        double dx = this.wantedX - this.bird.getX();
        double dy = this.wantedY - this.bird.getY();
        double dz = this.wantedZ - this.bird.getZ();
        double h = Math.sqrt(dx * dx + dz * dz);

        // 速さ: 状態ごとの巡航へ寄せる。止まり（HOVER）は 0 へ。行き先の手前 4 ブロックで少し緩める
        double cruise = st == ShearwaterEntity.HOVER ? 0.0
                : (st == ShearwaterEntity.GLIDE ? CRUISE_GLIDE : CRUISE_FLY) * this.speedModifier;
        if (h < 4.0 && st != ShearwaterEntity.HOVER) {
            cruise *= 0.6 + 0.1 * h;
        }
        double accel = st == ShearwaterEntity.GLIDE ? 0.006 : 0.01;
        this.speed += Mth.clamp(cruise - this.speed, -0.012, accel);

        // 向き: 行き先の方角へ、1 tick に（速さ ÷ 最小半径）まで
        if (h > 0.5) {
            float want = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
            float maxTurn = st == ShearwaterEntity.HOVER ? 4.0f
                    : (float) Math.max(1.5, Math.toDegrees(this.speed / MIN_TURN_RADIUS));
            this.bird.setYRot(this.rotlerp(this.bird.getYRot(), want, maxTurn));
        }

        // 上下: 差の 1/10 を目標の速さにして寄せる。上りは羽ばたきで 0.08、滑空は 0.05（速さを高さに換える分）まで
        double climbMax = st == ShearwaterEntity.GLIDE ? 0.05 : 0.08;
        double wantVy = Mth.clamp(dy * 0.1, -0.15, climbMax);
        if (st != ShearwaterEntity.LAND && !this.bird.isShearing()) {
            int top = this.bird.level().getHeight(Heightmap.Types.MOTION_BLOCKING,
                    this.bird.getBlockX(), this.bird.getBlockZ());
            double below = top + FLOOR - this.bird.getY();
            if (below > 0.0) {
                wantVy = Math.max(wantVy, Math.min(0.08, below * 0.2));
            }
        }
        this.vy += Mth.clamp(wantVy - this.vy, -0.02, 0.02);

        if (this.bird.horizontalCollision) {
            // 崖や島にぶつかったら、速さを落として上へ逃げる
            this.speed *= 0.5;
            this.vy = Math.max(this.vy, 0.08);
        }
        float yaw = this.bird.getYRot() * Mth.DEG_TO_RAD;
        this.bird.setDeltaMovement(-Mth.sin(yaw) * this.speed, this.vy, Mth.cos(yaw) * this.speed);
        this.bird.yBodyRot = this.bird.getYRot();
    }
}
