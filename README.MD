# GameDex
GameDex is an open source digital game collection management tool,
written in Kotlin and powered by [TornadoFX](https://github.com/edvin/tornadofx) & [JFoenix](https://github.com/jfoenixadmin/JFoenix)

It is currently in alpha, so there is no tutorial or binary distribution (yet), however you can [build it yourself](#build-distribution).

Here's how it looks like:

![GameDex Main Page](https://gitlab.com/ykrasik/gamedex/wikis/uploads/c07c1ba86490dabb8dc29a2c5c8ffb07/GameDex_Main.JPG)

# Features
* Digital game collection - define folders as game libraries and sync them with online game database providers. The sync process automatically determines newly added games.
* Currently integrated with IGDB.com & GiantBomb.com as providers - fill in your api key and fetch data about your games.
* Designed from the ground up to support multiple providers - as more provider plugins become available, GameDex will have access to more sources of game information.
* Supports multiple game platforms - Windows, Linux, Mac, Android.
* A beautiful game wall display to proudly show off your collection.
* A rich filter system that allows you to filter your games by various parameters and combine filters using boolean `and`, `or` & `not` operators for ultimate control.
* Create reports on your game collection to easily see all games that answer certain criteria in one place.
* Special reports for duplications in your collection and reports that show you how you should have named the game folder to be consistent with online information.
* Add tags (labels) to games and use these tags as filters.
* Supports re-fetching game information from providers, so you always have the most up-to-date game information locally.

# Build Distribution:
Prerequisites: JDK 8 only, does not support any of the later JDKs. In root folder:

`gradlew dist`

The distribution will be under project root in `dist` folder
and will contain a standalone copy of your JRE.
On windows, if you also have InnoSetup installed, it will generate a native windows setup file.

You can copy the distribution anywhere you like, it's portable.

# Run Distribution
On windows: `dist\GameDex\GameDex.exe`

# Run Demo Application
The demo application is intended for development purposes,
but you can use it to play around with GameDex without affecting any real world data.
It generates a game database filled with randomly generated games and
all providers are replaced with fake servers that emulate them.

From the command line `gradlew runDemo`

From the IDE, run `app\javafx\gamedex-javafx-app\src\test\kotlin\com\gitlab\ykrasik\gamedex\javafx\TestApplication.kt`

# Run Tests
For tests to pass, create a gradle.properties file under the root project with the following properties:
```
systemProp.gameDex.giantBomb.apiKey={yourApiKey}
systemProp.gameDex.igdb.apiKey={yourApiKey}
```
This is because contract tests for providers require real credentials to work.

# Maintenance Tasks

#### Create release tag
`gradlew tag -Prelease`

#### Check for library version updates
`gradlew versions`