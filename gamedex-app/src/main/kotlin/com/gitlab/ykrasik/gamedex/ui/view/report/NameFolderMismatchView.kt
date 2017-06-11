package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.core.GameNameFolderMismatch
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.ui.allowDeselection
import com.gitlab.ykrasik.gamedex.ui.customGraphicColumn
import com.gitlab.ykrasik.gamedex.ui.imageViewColumn
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import tornadofx.*

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 18:41
 */
class NameFolderMismatchView : ReportView<GameNameFolderMismatch>("Name-Folder Mismatch", Theme.Icon.report()) {
    private val providerRepository: GameProviderRepository by di()

    override val reportHeader get() = "Mismatches"
    override val ongoingReport get() = reportsController.nameFolderMismatches

    override fun reportsView() = tableview<GameNameFolderMismatch> {
        allowDeselection(onClickAgain = true)

        makeIndexColumn().apply { addClass(CommonStyle.centered) }
        imageViewColumn("Provider", fitHeight = 80.0, fitWidth = 160.0, isPreserveRatio = true) { mismatch ->
            providerRepository[mismatch.providerId].logoImage.toProperty()
        }
        customGraphicColumn("Expected Name") { mismatch -> label(mismatch.expectedName) }

        gamesTable.selectionModel.selectedItemProperty().onChange { selectedGame ->
            items = selectedGame?.let { ongoingReport.resultsProperty.value[it]!!.observable() }
            resizeColumnsToFitContent()
        }
    }
}