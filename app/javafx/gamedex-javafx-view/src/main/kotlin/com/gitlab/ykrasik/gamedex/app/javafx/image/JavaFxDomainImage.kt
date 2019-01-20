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

package com.gitlab.ykrasik.gamedex.app.javafx.image

import com.gitlab.ykrasik.gamedex.app.api.image.ImageFactory
import com.gitlab.ykrasik.gamedex.javafx.control.toImage
import javafx.scene.image.Image
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 11/04/2018
 * Time: 11:10
 */
class JavaFxDomainImage(override val raw: ByteArray, val image: Image) : DomainImage

val DomainImage.image get() = (this as JavaFxDomainImage).image

@Singleton
class JavaFxImageFactory : ImageFactory {
    override fun invoke(data: ByteArray) = JavaFxDomainImage(data, data.toImage())
}

typealias DomainImage = com.gitlab.ykrasik.gamedex.app.api.image.Image
typealias JavaFxImage = javafx.scene.image.Image