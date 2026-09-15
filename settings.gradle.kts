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
    }
}

rootProject.name = "weartube"

// shared : お気に入り・API キーの DataStore、YouTube Data API、Data Layer 同期（wear と mobile で共有）
// wear   : Wear OS アプリ（再生はすべてここで完結する）
// mobile : スマホの設定アプリ（API キー入力、お気に入り管理、時計への同期）
include(":shared")
include(":wear")
include(":mobile")
