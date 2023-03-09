# Change Log

## [0.5.0] 2023-03-09
* Major version upgrades for all libraries
* Use javapackager Gradle plugin instead of javafx-gradle-plugin, use 'why' for bundling on Windows
* Support detection & fixing of renamed / moved games as part of cleanup
* Adjust to latest OpenCritic changes
* Introduce automatic retries on provider server errors (5xx)
* Add a context menu to the Sync Games screen for existing games - can now edit/delete games right from that screen
* Allow specifying a command for opening an external browser when clicking on any link
* Sync only missing providers by default when re-syncing an existing game
* Also allow refreshing the preset result when re-syncing
* When deleting a game from the Game Details view, navigate to the next or prev game instead of immediately closing the view
* Add keybinds to actions:
  - 'esc' to cancel during provider search
  - 'delete' to delete from game details screen
* Limit poster size in Game Details View to be half the height of the screen, but it still goes off the screen sometimes...
* Only add the platform to the youtube search query if the platform is 'Android'
* Fix occasional out-of-bounds error in log view

## [0.4.1] 2020-11-21
* IGDB: Migrate to V4 API
* Upgrade embedded JRE version to 1.8.0_271
* Revert to embedded H2 version 1.4.196, a transitive dependency was pulling 1.4.199 which has performance issues on large dbs
* When updating a game, fix issue where provider data 'createDate' was being updated to current 'updateDate'
* When updating a game, only update its provider data 'updateDate' if the provider actually had updated data
* When updating a game, only update its global 'updateDate' if something actually changed
* Add warning icons to confirmation dialogs of risky actions
* Allow resuming bulk update operations that did not update all games (due to errors on some games, usually)
* Use Kotlin 1.4 SharedFlow instead of BroadcastChannel in a few places

## [0.4.0] 2020-06-06
#### Features
* Refactor the whole codebase to use Kotlin [Flow](https://kotlinlang.org/docs/reference/coroutines/flow.html)
* Add new provider: OpenCritic
* GiantBomb: Update to latest api changes about releaseDate
* GiantBomb: Add another image that should be treated as 'no-image'
* IGDB: Filter out scores with 0 votes
* Allow pagination in provider search (support offset-limit in provider api)
* Allow fetching a search result during provider game sync. Useful for OpenCritic
* Set max screenshots to 12
* Add screenshot url as tooltip to game details view
* Remove CriticScore & UserScore priorities, use score with the most votes instead
* Add version to preloader view
* Add path being searched to providerSearchView, and stop persisting thumbnails of search results
* Add ability to set a file as the main executable of the game, and execute the game by pressing a button
* Allow setting main executable file from the 'edit game' view
* Add a RichTextFx LogView, with a maximum of 1000 entries
* Every time game details are opened, trace the RawGame as a pretty json to the log
* Add some more default genre mappings
* Make default HttpClient timeouts much shorter
* Hide all currently showing overlays when pressing 'ctrl+f' to search
* Allow cleanup db view to display which images & file cache entries are stale
* Add ability to view raw game data as text
* Remove debounce when navigating games

#### Bugfixes
* Catch errors thrown while calculating FileTree
* Catch errors thrown while bulk updating games
* Fix filter, sort & displayType popOver arrow to always be Top Left
* Fix uncaught exception in RenameMove dialog when a communication error with the file system occurs
* Fix uncaught exception when excluding or skipping all providers in a provider sync operation
* Fix uncaught exception when cancelling bulk provider update Also update games by their update date, in increasing order.
* Do not catch exceptions thrown when updating a single game
* Limit game description width to 600, again
* IGDB: Add missing genres (Visual Novel, Card & Board Game, MOBA)
* Fix issue with cached image extensions sometimes having characters that are illegal in a file name

## [0.3.0] 2019-07-18
This release is NOT BACKWARDS COMPATIBLE with 0.2.0, there were internal changes to the database structure.
* Redesign windows - windows will now darken the content underneath them
* Redesign game details view - it now shows the game poster, screenshots & file tree
* Redesign main navigation menu - reorder items, make menus open after a hover delay and remove the 'Games' text label.
* Redesign main toolbar - Re-order buttons and hide platform selection button under the filter button
* Add ability to set an 'All' platform
* Redesign notifications - each notification now has a title and a white background
* Make scroll bars pretty - they now fade out and are only visible when in use or on hover
* Add a Game List View
* Allow saving filters and searching the saved filters
* Filters may now apply tags to games that match the filter
* Removed reports - List View & Saved Filters achieve the same purpose
* Duplicates Report & Folder Name Diff are now separate screens accessible as maintenance tasks instead of being filters
* Folder Name Diff can now ignore diffs if only a single provider reports
  it as a diff
* 'And' & 'Or' filters can now take multiple children instead of just 2
* Add a File Name filter
* Replace Null Critic Score & Null User Score filters with setting the target score to be less than 0 on the respective filters
* Add ability to import/export db, provider accounts & saved filters
* Add a 'quick matches' dropdown under the search textfield which displays games matching the search query (but not the current filters) and opens game details view when clicked
* Improve rename/move dialog - it now takes a free text path and detects the library to which it matches
* Upgrade to IGDB Api 3000
* Add a fallback to GiantBomb when the 'original_release_date' field is null
* Add an in-app browser and a setting which controls whether to use it or the system's native browser
* Rename Re-Download -> Update & Bulk Update
* Rename Re-Sync -> Sync & Sync Games with Missing Providers and add ability to only sync missing providers
* Disable updating & syncing games when it would result in an error (no enabled providers etc)
* Added Github & Gitlab links to about view
* Allow changing the type & platform of empty libraries
* Log all http requests & responses made through Ktor (all provider requests)
* Collapse log entries to one line by default & expand them on selection (hides exception stack traces by default)
* Store all images in a single folder cache/images
* Reduce image in-memory cache size to 20 - it was taking a lot of memory without giving any benefit

## [0.2.0] 2019-04-03
* Added a plugin system - plugins have their own classLoader & injector
* Implemented providers as plugins

## [0.1.0] 2019-02-09
* Initial release
