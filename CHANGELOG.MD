# Change Log

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