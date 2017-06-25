package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.core.ReportRule
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.ui.imageview
import com.gitlab.ykrasik.gamedex.ui.jfxButton
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import javafx.scene.control.TableView
import javafx.scene.layout.Priority
import tornadofx.*

/**
 * User: ykrasik
 * Date: 25/06/2017
 * Time: 09:48
 */
class DuplicationFragment(
    duplication: ReportRule.Rules.GameDuplication,
    gamesTable: TableView<Game>
) : Fragment() {
    private val providerRepository: GameProviderRepository by di()

    override val root = form {
        addClass(CommonStyle.centered)
        fieldset {
            inputGrow = Priority.ALWAYS
            field {
                imageview(providerRepository[duplication.providerId].logoImage) {
                    fitHeight = 80.0
                    fitWidth = 160.0
                    isPreserveRatio = true
                }
            }
            field {
                val game = duplication.duplicatedGame
                jfxButton(game.name) {
                    addClass(CommonStyle.fillAvailableWidth)
                    setOnAction {
                        gamesTable.selectionModel.select(game)
                        gamesTable.scrollTo(game)
                    }
                }
            }
        }
    }
}