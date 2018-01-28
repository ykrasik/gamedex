package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.core.Filter
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
    duplication: Filter.Duplications.GameDuplication,
    gamesTable: TableView<Game>
) : Fragment() {
    override val root = form {
        addClass(CommonStyle.centered)
        fieldset {
            inputGrow = Priority.ALWAYS
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