pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://repo.maven.apache.org/maven2/") }
        maven { url = uri("https://maven.myket.ir") }
        maven { url = uri("https://maven.myket.id") }
        maven { url = uri("https://jitpack.io") }
        maven { url= uri("https://maven.google.com") }
        maven("https://en-mirror.ir")
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }

        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://repo.maven.apache.org/maven2/") }
        maven { url = uri("https://maven.myket.ir") }
        maven { url = uri("https://maven.myket.id") }
        maven { url = uri("https://jitpack.io") }
        maven { url= uri("https://maven.google.com") }
        maven("https://en-mirror.ir")
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }



    }
}
rootProject.name = "GameHub"
include(":shared")
include(":host")
include(":games:tictactoe")
include(":games:uno")
include(":server")
include(":games:connectfour")
include(":games:ludo")
include(":games:monopoly")
include(":games:chess")
include(":games:farkle")
include(":games:esmofamil")
include(":games:backgammon")
include(":games:abalone")
include(":games:spades-baloot")
include(":games:othello")
include(":games:baltazar")
include(":games:bridge")
include(":games:checkers")
include(":games:blokus")
include(":games:yahtzee")
include(":games:nard")
include(":games:hex")
include(":games:battleship")
include(":games:match-monster")
include(":games:soccer-striker")
