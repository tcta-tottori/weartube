pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        // Android 系のグループだけ Google Maven を見る（無関係な依存で問い合わせない）
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // GeckoView は Maven Central に無く、Mozilla の Maven にしか置かれていない
        maven {
            url = uri("https://maven.mozilla.org/maven2/")
            content { includeGroupByRegex("org\\.mozilla.*") }
        }
    }
}

rootProject.name = "weartube"

// shared : お気に入り・API キーの DataStore、YouTube Data API、Data Layer 同期（wear と mobile で共有）
// wear   : Wear OS アプリ（再生はすべてここで完結する）
// mobile : スマホの設定アプリ（API キー入力、お気に入り管理、時計への同期）
// geckopoc : GeckoView の検証専用アプリ（本体には入れない）
include(":shared")
include(":wear")
include(":mobile")

// geckopoc : 再生方式の検証専用アプリ（design.md 12 章）。GeckoView が重いので本体とは別にする
include(":geckopoc")
