/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.javafx.report

import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.javafx.provider.logoImage
import com.gitlab.ykrasik.gamedex.javafx.toImageView
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import tornadofx.Fragment
import tornadofx.stackpane

/**
 * User: ykrasik
 * Date: 27/06/2017
 * Time: 21:01
 */
class ProviderLogoFragment(providerId: ProviderId) : Fragment() {
    private val gameProviderService: GameProviderService by di()

    override val root = stackpane {
        children += gameProviderService.provider(providerId).logoImage.toImageView(height = 80.0, width = 160.0)
    }
}