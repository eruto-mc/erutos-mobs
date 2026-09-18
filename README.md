# Eruto's Mobs

嵐の側の生き物たち。Forge 1.20.1・GeckoLib 4 が要る・当部自作（MIT）。

第一号は **ミズナギドリ**。海域にごく稀に 1 体だけ湧く伝説の海鳥で、海の上を滑空と羽ばたきで巡り、
水面に降りて浮き、夜は水面で眠り、飛び立つときは助走する。

## 作り

| 何 | どこ | 誰が作るか |
| - | - | - |
| 模型・絵・13 の動き | `src/main/resources/assets/erutosmobs/{geo,animations,textures}` | 手元の模型作りの道具 mc-model-kit（未公開）。その `ship_to_mod.py` が写す。ここでは手で触らない |
| 生き物の中身（状態・AI・湧き） | `src/main/java/net/erutobusiness/erutosmobs/entity/ShearwaterEntity.java` | この MOD |
| 描画（GeckoLib） | `client/ShearwaterModel.java`・`client/ShearwaterRenderer.java`（0.6 倍。翼幅 約 12 ブロック） | この MOD |
| 湧き | `data/erutosmobs/forge/biome_modifier/shearwater_spawns.json`（海・重み 1・1 体）＋ `checkShearwaterSpawn`（水面・空が見える・25 回に 1 回・192 ブロック以内に同族なし） | この MOD |
| 音 | `assets/erutosmobs/sounds/`（声 2・被弾・死・羽音。加算合成の笛としゃがれ声） | 手元の音の道具 wavs（未公開）の `projects/2026-09_erutos-mobs-sfx/make.py --ship` が写す |

## 状態と動き

| 状態 | 動き | いつ |
| - | - | - |
| FLY / GLIDE | swim / glide | 海の上を巡る。羽ばたき 2.5〜5 秒と滑空 4〜10 秒を交互に |
| HOVER | hover | 行き先が無く止まっているとき |
| DIVE → PADDLE | dive → paddle | 1.5〜4 分に 1 回、真下が海で近くに人が居なければ降りて浮く（15〜45 秒） |
| SLEEP | sleep | 水面で夜になったら。朝に戻る |
| TAKEOFF | takeoff | 浮いている時間が尽きたか、人が 6 ブロック以内に来たか、殴られたら |
| cry / hurt / faint | 一度きり | 30〜100 秒ごとに鳴く／被弾／死（1.6 秒倒れて消える） |
| LAND → STAND / WALK | dive → stand / walk | 休む 3 回に 1 回は、20 ブロック以内の水辺の砂・草へ降りて立ち、たまによちよち歩く（10〜35 秒）。人が 8 ブロックに来たら飛び立つ |

## 建て方

```bat
set JAVA_HOME=<JDK 17>
gradlew build
```

`build/libs/erutosmobs-<版>.jar` ができる。サーバと全員のクライアントに GeckoLib 4 が要る。

## 参考にしたもの

- 作りの手本: Alex's Mobs（LGPL）。1 体が何で出来ているか（entity・model・render・音・湧き・loot）を jar で数えて、同じ部品をそろえた。コードと絵は写していない。
- 生き物の暮らし: ja.wikipedia「オオミズナギドリ」／en.wikipedia「Shearwater」「Streaked shearwater」。
