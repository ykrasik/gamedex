# GameDex
GameDex is an open source digital game collection management application,
written in Kotlin and powered by [TornadoFX](https://github.com/edvin/tornadofx) & [JFoenix](https://github.com/jfoenixadmin/JFoenix)
         
# Download
You can [download it](https://github.com/ykrasik/gamedex/releases) or [build it](#build-distribution).

[Changelog](./CHANGELOG.MD)

There is currently no tutorial.

![GameDex Main Page](https://gitlab.com/ykrasik/gamedex/-/wikis/uploads/069cecaf2adac774392b8ead20e36aeb/GameDex.JPG)

# Features
* Supports multiple game platforms - Windows, Linux, Mac, Android.
* Define folders as game libraries and sync them with online game database providers. 
  * The sync process automatically determines newly added games.
* Comes with a rich filter system that allows filtering games by various parameters and combining filters using boolean `and`, `or` & `not` operators.
    * Can be used to create reports on your game collection.
* Supports IGDB.com, GiantBomb.com & OpenCritic as providers
    * IGDB & GiantBomb require an account for an API key.
* Choose between a beautiful wall display or a functional list display.
* Add tags (labels) to games.
* Comes with a duplication detection tool.
* Supports updating game information from providers, so you always have the most up-to-date game information locally.

# Build Distribution:
`gradlew dist`

Prerequisites: JDK 8 only, does not support any of the later JDKs.

The distribution will be under project root in `dist` folder
and will contain a standalone copy of your JRE.

You can copy the distribution anywhere you like, it's portable & self-contained.
                      
### Build Distribution with Installer
`gradlew dist -Pinstaller` on Windows.

It will generate a native windows setup file in `app/javafx/gamedex-javafx-app/build/GameDex_{version}.exe`

## Run Distribution
On windows: `dist\GameDex.exe`

# Run Demo Application
`gradlew runDemo`

The demo application is intended for development purposes,
but you can use it to play around with GameDex without affecting any real world data.
It generates a game database filled with randomly generated games and
all providers are replaced with fake servers that emulate them.

You can also run the demo application from the IDE - `app/javafx/gamedex-javafx-app/src/test/kotlin/com/gitlab/ykrasik/gamedex/javaf/TestApplication.kt`

# Run Tests
`gradlew test`

For tests to pass, you need to have the following entries in a [gradle.properties file](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties):
```
systemProp.gameDex.giantBomb.apiKey={yourApiKey}
systemProp.gameDex.igdb.clientId={yourClientId}
systemProp.gameDex.igdb.clientSecret={yourClientSecret}
```
This is because contract tests for providers require real credentials to work.

# Maintenance Tasks

#### Check for library version updates
`gradlew versions`
