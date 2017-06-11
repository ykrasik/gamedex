package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.core.GameDuplication
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.ui.allowDeselection
import com.gitlab.ykrasik.gamedex.ui.customGraphicColumn
import com.gitlab.ykrasik.gamedex.ui.imageViewColumn
import com.gitlab.ykrasik.gamedex.ui.jfxButton
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import com.gitlab.ykrasik.gamedex.ui.theme.pathButton
import tornadofx.*

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 11:32
 */
class DuplicateGamesView : ReportView<GameDuplication>("Duplicate Games", Theme.Icon.book()) {
    private val providerRepository: GameProviderRepository by di()

    override val reportHeader get() = "Duplications"
    override val ongoingReport get() = reportsController.duplications

    override fun reportsView() = tableview<GameDuplication> {
        allowDeselection(onClickAgain = true)

        makeIndexColumn().apply { addClass(CommonStyle.centered) }
        imageViewColumn("Provider", fitHeight = 80.0, fitWidth = 160.0, isPreserveRatio = true) { (providerId, _) ->
            providerRepository[providerId].logoImage.toProperty()
        }
        customGraphicColumn("Game") { (_, game) ->
            jfxButton(game.name) {
                setOnAction {
                    selectGame(game)
                    scrollTo(game)
                }
            }
        }
        customGraphicColumn("Path") { pathButton(it.duplicatedGame.path) }

        selectedGameProperty.onChange { selectedGame ->
            items = selectedGame?.let { ongoingReport.resultsProperty.value[it]!!.observable() }
            resizeColumnsToFitContent()
        }
    }
}