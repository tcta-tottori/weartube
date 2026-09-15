# WearTube

Pixel Watch（Wear OS 4 以降）単体で YouTube を再生する、個人利用専用アプリ。
仕様は [design.md](design.md)、実装ルールは [CLAUDE.md](CLAUDE.md) を参照。

- 再生は YouTube 公式の IFrame Player（WebView）を通す。ダウンロードやストリーム抽出はしない
- スマホ連携なし。音声出力は時計本体のスピーカーか、時計に直接ペアリングした BT イヤホン
- Play ストアには出さない。GitHub Actions でビルドした APK を adb で入れる

## 進捗（design.md 8 章）

- [x] Step 1 骨組み（Compose for Wear OS Material3、ホーム、DataStore）
- [x] Step 2 再生（WebView + IFrame Player、再生／一時停止）
- [x] Step 3 プレーヤー UI（全面黒 + 16:9 + 自動フェードのオーバーレイ）
- [x] Step 4 出力デバイス検出、オーディオフォーカス、リューズで音量
- [x] Step 5 Data API 検索（音声入力）、お気に入り
- [x] Step 6 シーク、エラーハンドリング

すべて **実機未確認**。特に「Wear OS に WebView があるか」「IFrame Player が動くか」「電池」は Pixel Watch で確かめる必要がある（design.md 7 章）。

## APK の入手とインストール

GitHub Actions が push のたびにビルドし、プレリリース [dev](https://github.com/tcta-tottori/weartube/releases/tag/dev) に置き換える。

| ファイル | 端末 |
| --- | --- |
| `weartube-wear-debug.apk` | Wear OS（Pixel Watch） |

```sh
# 時計: 設定 > システム > 開発者向けオプション > ADB デバッグ / 無線デバッグ を ON
adb pair <時計のIP>:<ペアリングポート>
adb connect <時計のIP>:5555
adb install -r weartube-wear-debug.apk
adb logcat | grep -i weartube
```

署名鍵はリポジトリの `keystore/debug.keystore` に固定してあるので、以後は上書きインストールで更新できる。

## YouTube Data API キー

検索には自分の Google Cloud プロジェクトで発行した YouTube Data API v3 のキーが要る（お気に入りと URL 入力からの再生はキー無しでも動く）。
キーはソースにコミットしない。次のどちらかで渡す。

1. **GitHub Actions の secret `YOUTUBE_API_KEY`**（推奨）— ビルド時に BuildConfig へ入る。時計での入力が不要
2. 時計の設定画面から入力（DataStore に保存）。ローカルビルドなら `local.properties` に `YOUTUBE_API_KEY=xxx` でも可

## ビルド・チェック

```sh
./gradlew ktlintCheck            # コードスタイル
./gradlew :app:assembleDebug     # Wear OS APK
```

Android Studio で開く場合は AGP 9.1 / Gradle 9.5 / compileSdk 37 に対応した版を使う。

## 構成

```
app/src/main/
├── kotlin/com/kazuya/weartube/
│   ├── ui/          Compose 画面（home / search / player / settings / navigation / common）
│   ├── player/      WebView ラッパー（PlayerController）、JS ブリッジ（PlayerBridge）
│   ├── audio/       出力デバイス検出、オーディオフォーカス、リューズ音量
│   ├── data/        DataStore（お気に入り・API キー）、YouTube Data API クライアント
│   ├── net/         ネットワーク接続の確認
│   ├── AppContainer.kt   手動 DI
│   └── MainActivity.kt
└── assets/player.html   IFrame Player を載せるページ
```
