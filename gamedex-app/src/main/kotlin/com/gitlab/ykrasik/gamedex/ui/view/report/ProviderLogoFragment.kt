package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.ProviderId
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.ui.imageview
import tornadofx.Fragment
import tornadofx.stackpane

/**
 * User: ykrasik
 * Date: 27/06/2017
 * Time: 21:01
 */
class ProviderLogoFragment(providerId: ProviderId) : Fragment() {
    private val providerRepository: GameProviderRepository by di()

    override val root = stackpane {
        imageview(providerRepository[providerId].logoImage) {
            fitHeight = 80.0
            fitWidth = 160.0
            isPreserveRatio = true
        }
    }
}