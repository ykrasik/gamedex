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

package com.gitlab.ykrasik.gamedex.javafx

import tornadofx.View
import tornadofx.vbox

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 12:05
 */
abstract class BaseFragmentTestApp : BaseTestApp<BaseFragmentTestApp.DummyView>(DummyView::class) {
    class DummyView : View() {
        override val root = vbox {}
    }

    override fun init(view: DummyView) {
        this@BaseFragmentTestApp.init()
        System.exit(0)
    }

    protected abstract fun init()
}