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

package com.gitlab.ykrasik.gamedex.core.image.presenter

import com.gitlab.ykrasik.gamedex.app.api.image.ImageGalleryView
import com.gitlab.ykrasik.gamedex.app.api.image.ImageType
import com.gitlab.ykrasik.gamedex.app.api.image.ViewImageParams
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
        private var currentIndex by view.currentImageIndex
        private var imageParams by view.imageParams

        init {
            currentIndex = -1
            view.imageParams *= ViewImageParams(imageUrl = "", imageUrls = emptyList(), imageType = ImageType.Thumbnail)
            view.imageParams.forEach { onParamsChanged(it) }
            view.viewNextImageActions.debounce(100).forEach { onViewNextImage() }
            view.viewPrevImageActions.debounce(100).forEach { onViewPrevImage() }
        }

        private fun onParamsChanged(params: ViewImageParams) {
            currentIndex = params.imageUrls.indexOfFirst { it == params.imageUrl }
            setCanNavigate()
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
            currentIndex += offset
            imageParams = imageParams.copy(imageUrl = imageParams.imageUrls[currentIndex])
            setCanNavigate()
        }

        private fun setCanNavigate() {
            view.canViewNextImage *= Try { check(currentIndex + 1 < imageParams.imageUrls.size) { "No more images!" } }
            view.canViewPrevImage *= Try { check(currentIndex - 1 >= 0) { "No more images!" } }
        }
    }
}