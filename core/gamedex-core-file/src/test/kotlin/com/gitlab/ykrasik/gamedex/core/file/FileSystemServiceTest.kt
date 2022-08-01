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

package com.gitlab.ykrasik.gamedex.core.file

import com.gitlab.ykrasik.gamedex.test.Spec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

/**
 * User: ykrasik
 * Date: 12/06/2017
 * Time: 20:56
 */
class FileSystemServiceTest : Spec<FileSystemServiceTest.Scope>() {
    init {
        describe("analyzeFileName") {
            describe("order") {
                itShould("correctly extract order") {
                    analyze("[1] Some Name [Some MetaTag] [1.2.3] More Text").order shouldBe "1"
                    analyze("[99999] Some Name [Some MetaTag] [1.2.3] More Text").order shouldBe "99999"
                }

                itShould("only consider integers enclosed in square brackets as order") {
                    analyze("[] Some Name").order shouldBe null
                    analyze("[1a] Some Name").order shouldBe null
                    analyze("[1.2] Some Name").order shouldBe null
                    analyze("1 Some Name").order shouldBe null
                }

                itShould("only extract order from the beginning of the string") {
                    analyze("t[1] Some Name [2]").order shouldBe null
                    analyze("t [1] Some [2] Name").order shouldBe null
                    analyze("Some [2] Name").order shouldBe null
                    analyze("Some Name [2]").order shouldBe null
                }
            }

            describe("version") {
                itShould("correctly handle all possible variations of version") {
                    fun testAnalyzeVersion(version: String) =
                        analyze("[2] Some Name [Some MetaTag] [$version] More Text").version shouldBe version

                    testAnalyzeVersion("123")
                    testAnalyzeVersion("1.2.3")
                    testAnalyzeVersion("v 1.2.3")
                    testAnalyzeVersion("v1.2.3")

                    testAnalyzeVersion("Alpha v 1.2.3")
                    testAnalyzeVersion("alpha v1.2.3")
                    testAnalyzeVersion("Alpha 3")
                    testAnalyzeVersion("a1.2.3")
                    testAnalyzeVersion("a3")

                    testAnalyzeVersion("Beta v 1.2.3")
                    testAnalyzeVersion("beta v1.2.3")
                    testAnalyzeVersion("b1.2.3")
                    testAnalyzeVersion("B123")

                    testAnalyzeVersion("Update v 1.2.3")
                    testAnalyzeVersion("update v1.2.3")
                    testAnalyzeVersion("u1.2.3")
                    testAnalyzeVersion("Update.5")

                    testAnalyzeVersion("20b")
                    testAnalyzeVersion("1.161107A")
                    testAnalyzeVersion("1.0u1")
                    testAnalyzeVersion("0.17r584")
                    testAnalyzeVersion("Alpha 0.16.H2")
                    testAnalyzeVersion("Beta")
                }

                itShould("correctly extract version when order & metaTag are present") {
                    analyze("[1] Some Name [Some Metatag] [1.2.3]").version shouldBe "1.2.3"
                    analyze("[1] Some Name [1.2.3] [Some Metatag]").version shouldBe "1.2.3"
                    analyze("[1] Some [1.2.3] Name [Some Metatag]").version shouldBe "1.2.3"
                    analyze("[1] [1.2.3] Some Name [Some Metatag]").version shouldBe "1.2.3"
                }

                itShould("correctly extract version when order & metaTag are absent") {
                    analyze("[1.2.3] Some Name More Text").version shouldBe "1.2.3"
                    analyze("Some [1.2.3] Name More Text").version shouldBe "1.2.3"
                    analyze("Some Name [1.2.3] More Text").version shouldBe "1.2.3"
                    analyze("Some Name More [1.2.3] Text").version shouldBe "1.2.3"
                    analyze("Some Name More Text [1.2.3]").version shouldBe "1.2.3"
                }

                itShould("correctly extract version when only order is present") {
                    analyze("[1] Some Name [1.2.3]").version shouldBe "1.2.3"
                    analyze("[1] Some [1.2.3] Name").version shouldBe "1.2.3"
                    analyze("[1] [1.2.3] Some Name").version shouldBe "1.2.3"
                }

                itShould("correctly extract version when only metaTag is present") {
                    analyze("Some Name [Some Metatag] [1.2.3]").version shouldBe "1.2.3"
                    analyze("Some Name [1.2.3] [Some Metatag]").version shouldBe "1.2.3"
                    analyze("Some [1.2.3] Name [Some Metatag]").version shouldBe "1.2.3"
                    analyze("[1.2.3] Some Name [Some Metatag]").version shouldBe "1.2.3"
                }

                itShould("ignore version not in square brackets") {
                    analyze("Game v 1.2.3").version shouldBe null
                }
            }

            describe("metaTag") {
                itShould("correctly extract metaTag in it's variations") {
                    analyze("[1] Some name [Collector's Edition] [1.2.3] More Text").metaTag shouldBe "Collector's Edition"
                    analyze("[2] Some name [Redux] [1.2.3] More Text").metaTag shouldBe "Redux"
                }

                itShould("correctly extract metaTag when order & version are present") {
                    analyze("[1] [metaTag] Some Name [1.2.3] More Text").metaTag shouldBe "metaTag"
                    analyze("[1] Some [metaTag] Name [1.2.3] More Text").metaTag shouldBe "metaTag"
                    analyze("[1] Some Name [metaTag] [1.2.3] More Text").metaTag shouldBe "metaTag"
                    analyze("[1] Some Name [1.2.3] [metaTag] More Text").metaTag shouldBe "metaTag"
                    analyze("[1] Some Name [1.2.3] More [metaTag] Text").metaTag shouldBe "metaTag"
                    analyze("[1] Some Name [1.2.3] More Text [metaTag]").metaTag shouldBe "metaTag"
                }

                itShould("correctly extract metaTag when order & version are absent") {
                    analyze("[metaTag] Some Name More Text").metaTag shouldBe "metaTag"
                    analyze("Some [metaTag] Name More Text").metaTag shouldBe "metaTag"
                    analyze("Some Name [metaTag] More Text").metaTag shouldBe "metaTag"
                    analyze("Some Name More [metaTag] Text").metaTag shouldBe "metaTag"
                    analyze("Some Name More Text [metaTag]").metaTag shouldBe "metaTag"
                }

                itShould("correctly extract metaTag when only order is present") {
                    analyze("[1] Some Name [metaTag]").metaTag shouldBe "metaTag"
                    analyze("[1] Some [metaTag] Name").metaTag shouldBe "metaTag"
                    analyze("[1] [metaTag] Some Name").metaTag shouldBe "metaTag"
                }

                itShould("correctly extract metaTag when only version is present") {
                    analyze("Some Name [1.2.3] [metaTag]").metaTag shouldBe "metaTag"
                    analyze("Some Name [metaTag] [1.2.3]").metaTag shouldBe "metaTag"
                    analyze("Some [metaTag] Name [1.2.3]").metaTag shouldBe "metaTag"
                    analyze("[metaTag] Some Name [1.2.3]").metaTag shouldBe "metaTag"
                }
            }

            describe("gameName") {
                itShould("correctly extract game name with spaces trimmed & collapsed") {
                    analyze("One [asd] Two [1.2.3] Three").processedName shouldBe "One Two Three"
                    analyze(" One  [asd]  Two  [1.2.3]  Three ").processedName shouldBe "One Two Three"
                    analyze(" One  Two [1.2.3]   [Four]  Three  Five").processedName shouldBe "One Two Three Five"
                    analyze("  [asd] One  Two Three  4 [1.2.3]  ").processedName shouldBe "One Two Three 4"
                }

                itShould("replace all instances of ' - ' with ': ' and collapse spaces in game name") {
                    analyze("Test - Game").processedName shouldBe "Test: Game"
                    analyze("Test - Game - More").processedName shouldBe "Test: Game: More"
                    analyze("Test  -  Game").processedName shouldBe "Test: Game"
                    analyze("Test  -  Game  -  More [asd]").processedName shouldBe "Test: Game: More"
                    analyze("Test  -  Game  - [asd] More").processedName shouldBe "Test: Game: More"
                    analyze("Test  - [1.2.3] Game  -  More ").processedName shouldBe "Test: Game: More"
                    analyze("[1.2.3]  Test  -  Game  -  More  [asd]").processedName shouldBe "Test: Game: More"
                }

                itShould("only replace exact matches of ' - ' with ': '") {
                    analyze("Test-Game").processedName shouldBe "Test-Game"
                    analyze("Test- Game").processedName shouldBe "Test- Game"
                    analyze("Test -Game").processedName shouldBe "Test -Game"
                    analyze("Test -- Game").processedName shouldBe "Test -- Game"
                }
            }
        }
    }

    override fun scope() = Scope()
    class Scope {
        val fileSystemService = FileSystemServiceImpl(mockk(relaxed = true), mockk(relaxed = true))

        fun analyze(name: String) = fileSystemService.analyzeFolderName(name)
    }
}
