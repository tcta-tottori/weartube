# WearTube

Pixel Watch（Wear OS 4 以降）単体で YouTube を再生する、個人利用専用アプリ。
仕様は [design.md](design.md)、実装ルールは [CLAUDE.md](CLAUDE.md) を参照。

- 再生は YouTube 公式の IFrame Player（WebView）を通す。ダウンロードやストリーム抽出はしない
- スマホ側は設定アプリだけ。API キーとお気に入りを Wearable Data Layer で時計に同期する（時計はスマホ無しでも動く）
- 音声出力は時計本体のスピーカーか、時計に直接ペアリングした BT イヤホン
- Play ストアには出さない。GitHub Actions でビルドした APK を adb で入れる

## 進捗（design.md 8 章）

- [x] Step 1 骨組み（Compose for Wear OS Material3、ホーム、DataStore）
- [x] Step 2 再生（WebView + IFrame Player、再生／一時停止）
- [x] Step 3 プレーヤー UI（全面黒 + 16:9 + 自動フェードのオーバーレイ）
- [x] Step 4 出力デバイス検出、オーディオフォーカス、リューズで音量
- [x] Step 5 Data API 検索（音声入力）、お気に入り
- [x] Step 6 シーク、エラーハンドリング
- [x] スマホ設定アプリ（API キー、お気に入り、共有からの追加、時計との双方向同期）

すべて **実機未確認**。特に「Wear OS に WebView があるか」「IFrame Player が動くか」「電池」は Pixel Watch で確かめる必要がある（design.md 7 章）。

## APK の入手とインストール

GitHub Actions が push のたびにビルドし、プレリリース [dev](https://github.com/tcta-tottori/weartube/releases/tag/dev) に置き換える。

| ファイル | 端末 |
| --- | --- |
| `weartube-wear-debug.apk` | Wear OS（Pixel Watch） |
| `weartube-phone-debug.apk` | スマホの設定アプリ（ダウンロードして開くだけ） |

```sh
# 時計: 設定 > システム > 開発者向けオプション > ADB デバッグ / 無線デバッグ を ON
adb pair <時計のIP>:<ペアリングポート>
adb connect <時計のIP>:5555
adb install -r weartube-wear-debug.apk
adb logcat | grep -i weartube
```

署名鍵はリポジトリの `keystore/debug.keystore` に固定してあるので、以後は上書きインストールで更新できる。
スマホ版と時計版は同じ鍵・同じ applicationId なので、Wearable Data Layer の同期が成立する。

## YouTube Data API キー

検索には自分の Google Cloud プロジェクトで発行した YouTube Data API v3 のキーが要る（お気に入りと URL 入力からの再生はキー無しでも動く）。
キーはソースにコミットしない。次のどちらかで渡す。

1. **スマホの設定アプリに貼り付ける**（推奨）— ペアリング中の時計に自動で同期される
2. GitHub Actions の secret `YOUTUBE_API_KEY` — ビルド時に BuildConfig へ入る
3. 時計の設定画面から入力。ローカルビルドなら `local.properties` に `YOUTUBE_API_KEY=xxx` でも可

お気に入りも同様にスマホから追加できる。YouTube アプリの「共有」で「WearTube 設定」を選ぶか、URL を貼り付ける。

## ビルド・チェック

```sh
./gradlew ktlintCheck                                # コードスタイル
./gradlew :wear:assembleDebug :mobile:assembleDebug  # 時計 / スマホ APK
```

Android Studio で開く場合は AGP 9.1 / Gradle 9.5 / compileSdk 37 に対応した版を使う。

## 構成

```
shared/   共通ライブラリ: VideoItem、DataStore（お気に入り・API キー）、YouTube Data API、Data Layer 同期
wear/     時計アプリ: ui/（画面）、player/（WebView ラッパー・JS ブリッジ）、audio/、net/、sync/、assets/player.html
mobile/   スマホの設定アプリ: 1 画面（API キー・同期状態・お気に入り）、共有インテントの受け口、受信サービス
```
