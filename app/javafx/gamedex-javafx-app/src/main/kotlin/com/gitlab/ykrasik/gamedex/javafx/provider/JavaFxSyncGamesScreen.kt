/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.gitlab.ykrasik.gamedex.javafx.provider

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.LibraryPath
import com.gitlab.ykrasik.gamedex.app.api.file.ViewCanBrowseFile
import com.gitlab.ykrasik.gamedex.app.api.library.ViewCanAddLibrary
import com.gitlab.ykrasik.gamedex.app.api.library.ViewCanDeleteLibrary
import com.gitlab.ykrasik.gamedex.app.api.library.ViewCanEditLibrary
import com.gitlab.ykrasik.gamedex.app.api.library.ViewWithLibraries
import com.gitlab.ykrasik.gamedex.app.api.provider.GameSearchState
import com.gitlab.ykrasik.gamedex.app.api.provider.GameSearchStatus
import com.gitlab.ykrasik.gamedex.app.api.provider.SyncGamesView
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanSyncLibraries
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.game.search.SearchResultsFragment
import com.gitlab.ykrasik.gamedex.javafx.theme.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import com.gitlab.ykrasik.gamedex.util.IsValid
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.TreeItem
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 17/10/2018
 * Time: 09:18
 */
class JavaFxSyncGamesScreen : PresentableScreen("Sync", Icons.sync),
    ViewCanSyncLibraries,
    SyncGamesView,
    ViewWithLibraries,
    ViewCanAddLibrary,
    ViewCanEditLibrary,
    ViewCanDeleteLibrary,
    ViewCanBrowseFile {

    override val canSyncLibraries = state(IsValid.valid)
    override val syncLibrariesActions = channel<Unit>()
    override val cancelActions = channel<Unit>()

    override val isAllowSmartChooseResults = state(false)

    override val isGameSyncRunning = state(false)

    override val pathsToProcess = mutableListOf<LibraryPath>().observable()
    private val pathsToProcessSize = pathsToProcess.sizeProperty
    override val numProcessed = state(0)

    override val state = mutableMapOf<LibraryPath, GameSearchState>().observable()

    override val currentLibraryPath = userMutableState<LibraryPath?>(null)
    override val restartLibraryPathActions = channel<LibraryPath>()

    override val libraries = mutableListOf<Library>().observable()

    override val canAddLibraries = state(IsValid.valid)
    override val addLibraryActions = channel<Unit>()

    override val canEditLibraries = state(IsValid.valid)
    override val editLibraryActions = channel<Library>()

    override val canDeleteLibraries = state(IsValid.valid)
    override val deleteLibraryActions = channel<Library>()

    override val browseToFileActions = channel<File>()

    private val providerSearchView: SearchResultsFragment by inject()

    init {
        register()
    }

    private val pathTree = jfxTreeView<Any> {
        useMaxWidth = true
        hgrow = Priority.ALWAYS
        style {
            fontSize = 18.px
        }
        keepSelectionInView()

        setCellFactory {
            object : NotBuggedJFXTreeCell<Any>() {
                override fun updateItem(item: Any?, empty: Boolean) {
                    super.updateItem(item, empty)
                    when (item) {
                        null -> return
                        is LibraryPath -> {
                            addClass(CommonStyle.jfxHoverable)
                            text = item.relativePath.toString()
                        }
                        is Library -> {
                            removeClass(CommonStyle.jfxHoverable)
                            text = item.path.toString()
                            graphic = hbox(spacing = 2) {
                                alignment = Pos.CENTER_LEFT
                                add(item.platform.logo)
                                add(Icons.folder)
                            }
                        }
                        else -> {
                            removeClass(CommonStyle.jfxHoverable)
                            text = item.toString()
                            graphic = Icons.hdd
                        }
                    }

                    val cellPrefWidth = prefWidth(-1.0) + (verticalScrollbar?.width ?: 0.0) + insets.left + insets.right
                    if (cellPrefWidth > this@jfxTreeView.prefWidth) {
                        this@jfxTreeView.prefWidth = cellPrefWidth
                    }
                }
            }
        }

        root = TreeItem<Any>("Libraries").apply { isExpanded = true }

        fun LibraryPath.toLibraryTreeItem(): TreeItem<Any> =
            root.children.find { (it.value as Library) == library } ?: root.treeitem(library) { isExpanded = true }

        fun LibraryPath.findTreeItem(): TreeItem<Any> = toLibraryTreeItem().find(this)!!

        selectionModel.selectedItemProperty().onChange { selectedItem ->
            (selectedItem?.value as? LibraryPath)?.let { libraryPath ->
                currentLibraryPath.valueFromView = libraryPath
            }
        }

        currentLibraryPath.onChange { currentLibraryPath ->
            if (currentLibraryPath != null && selectionModel.selectedItem?.value as? LibraryPath != currentLibraryPath) {
                val item = currentLibraryPath.findTreeItem()
                selectionModel.select(item)
            }
        }

        pathsToProcess.onChange { change ->
            while (change.next()) {
                when {
                    change.wasAdded() -> {
                        change.addedSubList.forEach { libraryPath ->
                            val libraryTreeItem = libraryPath.toLibraryTreeItem()
                            libraryTreeItem.treeitem(libraryPath)
                        }
                    }
                    change.wasRemoved() -> {
                        change.removed.forEach { libraryPath ->
                            val libraryTreeItem = libraryPath.toLibraryTreeItem()
                            libraryTreeItem.children.remove(libraryTreeItem.find(libraryPath)!!)
                            if (libraryTreeItem.children.isEmpty()) {
                                libraryTreeItem.parent.children.remove(libraryTreeItem)
                            }
                        }
                    }
                }
            }
        }

        state.onChange { change ->
            if (change.wasAdded()) {
                val state = change.valueAdded
                val libraryPath = change.key
                val treeItem = libraryPath.findTreeItem()
                treeItem.graphic = StackPane().apply {
                    jfxButton {
                        mouseTransparentWhen { isGameSyncRunning.property.not().or(!state.isFinished) }
                        graphicProperty().bind(hoverProperty().binding { hover ->
                            when {
                                hover && state.isFinished -> Icons.resetToDefault
                                state.status == GameSearchStatus.Success -> Icons.accept
                                state.status == GameSearchStatus.Cancelled -> Icons.cancel
                                else -> null
                            }?.size(24)
                        })
                        action(restartLibraryPathActions) { libraryPath }
                    }
                }
            }
        }
    }

    override val root = hbox {
        useMaxWidth = true
        visibleWhen { pathsToProcessSize.booleanBinding { it!!.toInt() > 0 } }

        add(pathTree)
        addComponent(providerSearchView) {
            root.useMaxWidth = true
            root.hgrow = Priority.ALWAYS
        }
    }

    override fun HBox.buildToolbar() {
        librariesMenu()
        gap()
        syncLibrariesSection()
    }

    private fun EventTarget.librariesMenu() = buttonWithPopover(graphic = Icons.hdd) {
        libraries.perform { libraries ->
            replaceChildren {
                gridpane {
                    hgap = 5.0
                    vgap = 3.0
                    libraries.forEach { library ->
                        row {
                            children += library.platform.logo
                            text(library.name)
                            jfxButton(library.path.toString()) {
                                useMaxWidth = true
                                addClass(CommonStyle.toolbarButton)
                                action(browseToFileActions) { library.path }
                            }
                            editButton {
                                removeClass(CommonStyle.toolbarButton)
                                enableWhen(canEditLibraries)
                                tooltip("Edit library '${library.name}'")
                                action(editLibraryActions) { library }
                            }
                            deleteButton {
                                removeClass(CommonStyle.toolbarButton)
                                enableWhen(canDeleteLibraries)
                                tooltip("Delete library '${library.name}'")
                                action(deleteLibraryActions) { library }
                            }
                        }
                    }
                }
                verticalGap()
                addButton {
                    useMaxWidth = true
                    hgrow = Priority.ALWAYS
                    alignment = Pos.CENTER
                    addClass(CommonStyle.thinBorder)
                    tooltip("Add a new library")
                    enableWhen(canAddLibraries)
                    action(addLibraryActions)
                }
            }
        }
    }.apply {
        addClass(CommonStyle.thinBorder)
        textProperty().bind(libraries.sizeProperty.stringBinding { "Libraries: $it" })
    }

    private fun EventTarget.syncLibrariesSection() = defaultHbox(spacing = 20) {
        useMaxWidth = true
        hgrow = Priority.ALWAYS

        val resultsAndNumProcessed = pathsToProcessSize.combineLatest(numProcessed.property)
        val numProcessedLabelProperty = resultsAndNumProcessed.stringBinding {
            val (numResults, numProcessed) = it!!
            "$numProcessed/$numResults"
        }
        val progressProperty = resultsAndNumProcessed.doubleBinding {
            val (numResults, numProcessed) = it!!
            numProcessed.toDouble() / numResults.toDouble()
        }

        isGameSyncRunning.property.perform { isGameSyncRunning ->
            replaceChildren {
                if (isGameSyncRunning) {
                    dangerButton("Stop Sync", graphic = Icons.stop) {
                        isCancelButton = false
                        action(cancelActions)
                    }
                    label(numProcessedLabelProperty)
                    jfxProgressBar(progressProperty) {
                        hgrow = Priority.ALWAYS
                        useMaxWidth = true
                        paddingRight = 20
                    }
                } else {
                    infoButton("Sync Libraries", graphic = Icons.folderSync) {
                        tooltip("Scan all libraries for new games")
                        enableWhen(canSyncLibraries)
                        action(syncLibrariesActions)
                    }
                }
            }
        }
    }

    private fun TreeItem<Any>.find(path: LibraryPath): TreeItem<Any>? =
        if (this.value as? LibraryPath == path) {
            this
        } else {
            children.asSequence()
                .mapNotNull { child -> child.find(path) }
                .firstOrNull()
        }

    override fun successMessage(message: String) = notification("Done: $message").info.show()
    override fun cancelledMessage(message: String) = notification("Cancelled: $message").error.show()
}