package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.core.GameNameFolderDiff
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import javafx.scene.layout.VBox
import tornadofx.*

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 18:41
 */
class NameFolderDiffView : ReportView<GameNameFolderDiff>("Name-Folder Diff", Theme.Icon.book()) {
    private val providerRepository: GameProviderRepository by di()
    private val gameSettings: GameSettings by di()

    override val reportHeader get() = "Differences"
    override val ongoingReport get() = reportsController.nameFolderDiffs

    override fun reportsView() = tableview<GameNameFolderDiff> {
        imageViewColumn("Provider", fitHeight = 80.0, fitWidth = 160.0, isPreserveRatio = true) { diff ->
            providerRepository[diff.providerId].logoImage.toProperty()
        }
        customGraphicColumn("Difference") { diff ->
            Form().apply {
                addClass(CommonStyle.centered)
                fieldset {
                    field("Expected") { children += DiffRenderer.renderExpected(diff) }
                    field("Actual") { children += DiffRenderer.renderActual(diff) }
                }
            }
        }

        selectedGameProperty.onChange { selectedGame ->
            items = selectedGame?.let { ongoingReport.resultsProperty.value[it]!!.observable() }
            resizeColumnsToFitContent()
        }

        popoverContextMenu {
            jfxButton("Rename to Expected", Theme.Icon.folder()) {
                setOnAction {
                    gameController.renameFolder(selectedGame, selectionModel.selectedItem.expected)
                }
            }
        }

        onUserSelect { gameController.renameFolder(selectedGame, it.expected) }
    }

    override val extraOptions = VBox().apply {
        jfxToggleButton {
            text = "Ignore version metadata"
            selectedProperty().bindBidirectional(gameSettings.nameFolderDiffIgnoreVersionMetadataProperty)
        }
    }

    init {
        gameSettings.nameFolderDiffIgnoreVersionMetadataProperty.onChange {
            ongoingReport.reload()
        }
    }
}