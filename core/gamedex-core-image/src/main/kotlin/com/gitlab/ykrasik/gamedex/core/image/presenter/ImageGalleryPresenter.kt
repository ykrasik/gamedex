/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.image.presenter

import com.gitlab.ykrasik.gamedex.app.api.image.ImageGalleryView
import com.gitlab.ykrasik.gamedex.app.api.util.debounce
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.util.Try
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 12/05/2019
 * Time: 09:15
 */
@Singleton
class ImageGalleryPresenter @Inject constructor() : Presenter<ImageGalleryView> {
    override fun present(view: ImageGalleryView) = object : ViewSession() {
        init {
            view.imageParams.forEach { params ->
                view.currentImageIndex.value = params.imageUrls.indexOfFirst { it == params.imageUrl }
            }
            view.currentImageIndex.forEach { currentImageIndex ->
                view.canViewNextImage *= Try { check(currentImageIndex + 1 < view.imageParams.value.imageUrls.size) { "No more images!" } }
                view.canViewPrevImage *= Try { check(currentImageIndex - 1 >= 0) { "No more images!" } }
            }

            view.viewNextImageActions.subscribe().debounce(100).forEach { onViewNextImage() }
            view.viewPrevImageActions.subscribe().debounce(100).forEach { onViewPrevImage() }
        }

        private fun onViewNextImage() {
            view.canViewNextImage.assert()
            navigate(+1)
        }

        private fun onViewPrevImage() {
            view.canViewPrevImage.assert()
            navigate(-1)
        }

        private fun navigate(offset: Int) {
            view.currentImageIndex.value += offset
            view.imageParams.modify { copy(imageUrl = imageUrls[view.currentImageIndex.value]) }
        }
    }
}