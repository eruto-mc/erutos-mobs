# Eruto's Mobs

嵐の側の生き物たち。Forge 1.20.1・GeckoLib 4 が要る・当部自作（MIT）。

第一号は **ミズナギドリ**。海域にごく稀に 1 体だけ湧く伝説の海鳥で、海の上を滑空と羽ばたきで巡り、
ときどき水面すれすれに傾いて翼端で波を切る。水面に降りて浮き、夜は水面で眠り、飛び立つときは助走する。
嵐の側の生き物なので、夜と雷雨で体の縁が光り、雷雨の中では自分へ雷を呼んで帯電する。

## 作り

| 何 | どこ | 誰が作るか |
| - | - | - |
| 模型・絵・13 の動き | `src/main/resources/assets/erutosmobs/{geo,animations,textures}` | 手元の模型作りの道具 mc-model-kit（未公開）。その `ship_to_mod.py` が写す。ここでは手で触らない |
| 生き物の中身（状態・AI・湧き） | `src/main/java/net/erutobusiness/erutosmobs/entity/ShearwaterEntity.java` | この MOD |
| 飛び方 | `entity/ShearwaterMoveControl.java`（向き・速さ・上下を自分で持つ）・`entity/ShearPassGoal.java`（波を切る） | この MOD |
| 描画（GeckoLib） | `client/ShearwaterModel.java`・`client/ShearwaterRenderer.java`（0.6 倍。翼幅 約 12 ブロック） | この MOD |
| 光る層・粒子 | `client/ShearwaterGlowLayer.java`・`client/FeatherParticle.java`・`client/MistParticle.java`・`entity/ShearwaterEffects.java` | この MOD。光る部分の絵（`textures/entity/shearwater_glow.png`）と羽根の絵（`textures/particle/`）は mc-model-kit が塗る |
| 粒子の出どころ | `entity/ShearwaterLocators.java`（翼端・足・翼の後縁の位置） | mc-model-kit が模型から計算して書く。手で直さない |
| 湧き | `data/erutosmobs/forge/biome_modifier/shearwater_spawns.json`（海・重み 1・1 体）＋ `checkShearwaterSpawn`（水面・空が見える・25 回に 1 回・192 ブロック以内に同族なし） | この MOD |
| 音 | `assets/erutosmobs/sounds/`（声 2・被弾・死・羽音。加算合成の笛としゃがれ声） | 手元の音の道具 wavs（未公開）の `projects/2026-09_erutos-mobs-sfx/make.py --ship` が写す |

## 状態と動き

| 状態 | 動き | いつ |
| - | - | - |
| FLY / GLIDE | swim / glide（曲がるときは glide_bank_l / _r） | 海の上を途切れずに巡る。上るときと遅いときは羽ばたき、ほかは滑空を基本に 5〜12 秒ごとに 2〜3 打だけ羽ばたく。曲がる半径は 6 ブロック以上で、曲がる側へ傾く。水面・地面の 1.5 ブロックより下へは降りない |
| 波を切る | glide_bank_l / _r | 45〜90 秒に 1 回、水面すれすれへ降り、傾いたまま半径 11〜15 ブロックの円を 7〜11 秒滑る。下がった翼端だけが水に入る |
| HOVER | hover | 行き先に着いたとき 8 回に 1 回（続けては 20 秒空ける）、その場で 2〜4 秒羽ばたいて止まる（餌を探す） |
| DIVE → PADDLE | dive → paddle | 1.5〜4 分に 1 回、真下が海で近くに人が居なければ降りて浮く（15〜45 秒）。雷雨の間は降りない |
| SLEEP | sleep | 水面で夜（時刻 13000〜23000）になったら。朝に戻る |
| TAKEOFF | takeoff | 浮いている時間が尽きたか、人が 6 ブロック以内に来たか、殴られたか、雷雨が来たら（眠っていても） |
| cry / hurt / faint | 一度きり | 30〜100 秒ごとに鳴く（飛んでいる昼は 4 回に 1 回。夜と雷雨は毎回）／被弾／死（1.6 秒倒れて消える） |
| LAND → STAND / WALK | dive → stand / walk | 休む 3 回に 1 回は、20 ブロック以内の水辺の砂・草へ降りて立ち、たまによちよち歩く（10〜35 秒）。人が 8 ブロックに来たら飛び立つ |

## 光・粒子・雷

| 何 | いつ | どう見える |
| - | - | - |
| 光る層 | いつも（昼はほぼ消える） | 琥珀色の縁・目・顔の泡の線が暗さに応じて光る（昼 0.06 → 夜 0.8）。雷雨でさらに強まり、ときどき瞬く |
| 鳴いた光 | 鳴くたび | 鳴きの 1.2 秒の間、光が一度強まる |
| 帯電 | 雷を受けたとき・嵐で雷を呼んだとき（1 分） | 雷で傷まず燃えず、体力が 10 戻る。光が最大近くになり、翼の後縁と翼端から火花が散る |
| 嵐の雷呼び | 雷雨の中を飛んでいる間、平均 90 秒に 1 回 | 見た目だけの雷（火も傷も無い）を自分へ落とし、帯電する |
| 風の筋 | 速く滑空しているとき | 両方の翼端から淡い白の筋が後ろへ引かれる |
| 波を切るしぶき | 傾いて水面すれすれを滑るとき | 下がった翼端が水面に触れた所から、しぶきと水の音 |
| 足の水しぶき・水滴 | 水面から飛び立つとき | 助走の足が水を蹴る拍にしぶき。飛び立ったあと 5 秒、翼の後縁から水滴が落ちる |
| 航跡・着水 | 水面を漕ぐとき・降りて水に入った瞬間 | 尾の後ろに航跡。着水は大きなしぶきと泡 |
| 羽根 | 殴られたとき（10 枚）・死んだとき（30 枚） | 嵐色の羽根がゆっくり揺れながら舞い落ちる |

## 設定

`config/erutosmobs-common.toml` の `[shearwater]`。

| 鍵 | 既定 | 意味 |
| - | - | - |
| `spawnOneIn` | 25 | 自然に湧くのは N 回に 1 回（海の湧きの重みに加えて） |
| `minDistanceBetween` | 192 | この距離の中に同族が居れば湧かない |
| `despawnDistance` | 256 | 全員がこれより遠いときだけ消える（バニラの水の生き物は 128） |
| `stormLightningOneIn` | 1800 | 雷雨の中を飛んでいる間、1 tick に 1/N で自分へ見た目だけの雷を呼ぶ。0 で呼ばない |

## 建て方

```bat
set JAVA_HOME=<JDK 17>
gradlew build
```

`build/libs/erutosmobs-<版>.jar` ができる。サーバと全員のクライアントに GeckoLib 4 が要る。

## 参考にしたもの

- 作りの手本: Alex's Mobs（LGPL）。1 体が何で出来ているか（entity・model・render・音・湧き・loot）を jar で数えて、同じ部品をそろえた。
  飛び方（行き先へ向けて速度に足し込み、向きは速度に合わせる）と光る層（目の描き方で重ねる）は、タイヨウチョウの作りを jar で読んで倣った。コードと絵は写していない。
- 生き物の暮らし: ja.wikipedia「オオミズナギドリ」／en.wikipedia「Shearwater」「Streaked shearwater」。
