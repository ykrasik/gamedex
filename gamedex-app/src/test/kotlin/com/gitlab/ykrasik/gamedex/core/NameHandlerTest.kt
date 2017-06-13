package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.test.ScopedWordSpec
import io.kotlintest.matchers.shouldBe

/**
 * User: ykrasik
 * Date: 12/06/2017
 * Time: 20:56
 */
class NameHandlerTest : ScopedWordSpec()  {
    init {
        "NameHandler" should {
            "analyze" should {
                "correctly extract version in its variations" {
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

                "correctly extract all possible locations of version when metaTag is also present" {
                    analyze("[1.2.3] Some Name [Some Metatag] More Text").version shouldBe "1.2.3"
                    analyze("Some [1.2.3] Name [Some Metatag] More Text").version shouldBe "1.2.3"
                    analyze("Some Name [1.2.3] [Some Metatag] More Text").version shouldBe "1.2.3"
                    analyze("Some Name [Some Metatag] [1.2.3] More Text").version shouldBe "1.2.3"
                    analyze("Some Name [Some Metatag] More [1.2.3] Text").version shouldBe "1.2.3"
                    analyze("Some Name [Some Metatag] More Text [1.2.3]").version shouldBe "1.2.3"
                }

                "correctly extract all possible locations of version when metaTag is not present" {
                    analyze("[1.2.3] Some Name More Text").version shouldBe "1.2.3"
                    analyze("Some [1.2.3] Name More Text").version shouldBe "1.2.3"
                    analyze("Some Name [1.2.3] More Text").version shouldBe "1.2.3"
                    analyze("Some Name More [1.2.3] Text").version shouldBe "1.2.3"
                    analyze("Some Name More Text [1.2.3]").version shouldBe "1.2.3"
                }

                "ignore version not in square brackets" {
                    analyze("Game v 1.2.3").version shouldBe null
                }

                "correctly extract metaTag in it's variations" {
                    testAnalyzeMetaTag("Collector's Edition")
                    testAnalyzeMetaTag("Redux")
                }

                "correctly extract all possible locations of metaTag when version is also present" {
                    analyze("[asd] Some Name [1.2.3] More Text").metaTag shouldBe "asd"
                    analyze("Some [asd] Name [1.2.3] More Text").metaTag shouldBe "asd"
                    analyze("Some Name [asd] [1.2.3] More Text").metaTag shouldBe "asd"
                    analyze("Some Name [1.2.3] [asd] More Text").metaTag shouldBe "asd"
                    analyze("Some Name [1.2.3] More [asd] Text").metaTag shouldBe "asd"
                    analyze("Some Name [1.2.3] More Text [asd]").metaTag shouldBe "asd"
                }

                "correctly extract all possible locations of metaTag when version is absent" {
                    analyze("[asd] Some Name More Text").metaTag shouldBe "asd"
                    analyze("Some [asd] Name More Text").metaTag shouldBe "asd"
                    analyze("Some Name [asd] More Text").metaTag shouldBe "asd"
                    analyze("Some Name More [asd] Text").metaTag shouldBe "asd"
                    analyze("Some Name More Text [asd]").metaTag shouldBe "asd"
                }

                "correctly extract game name with spaces trimmed & collapsed" {
                    analyze("One [asd] Two [1.2.3] Three").gameName shouldBe "One Two Three"
                    analyze(" One  [asd]  Two  [1.2.3]  Three ").gameName shouldBe "One Two Three"
                    analyze(" One  Two [1.2.3]   [Four]  Three  Five").gameName shouldBe "One Two Three Five"
                    analyze("  [asd] One  Two Three  4 [1.2.3]  ").gameName shouldBe "One Two Three 4"
                }

                "replace all instances of ' - ' with ': ' in game name" {
                    analyze("Test - Game").gameName shouldBe "Test: Game"
                    analyze("Test - Game - More").gameName shouldBe "Test: Game: More"
                    analyze("Test  -  Game").gameName shouldBe "Test: Game"
                    analyze("Test  -  Game  -  More [asd]").gameName shouldBe "Test: Game: More"
                }

                "replace all instances of ' - ' with ': ' in game name when version is present" {
                    analyze("Test  - [1.2.3] Game  -  More ").gameName shouldBe "Test: Game: More"
                }

                "replace all instances of ' - ' with ': ' in game name when metaTag is present" {
                    analyze("Test  -  Game  - [asd] More").gameName shouldBe "Test: Game: More"
                }

                "replace all instances of ' - ' with ': ' in game name when both version & metaTag are present" {
                    analyze("[1.2.3]  Test  -  Game  -  More  [asd]").gameName shouldBe "Test: Game: More"
                }

                "only replace exact matches of ' - ' with ': '" {
                    analyze("Test-Game").gameName shouldBe "Test-Game"
                    analyze("Test- Game").gameName shouldBe "Test- Game"
                    analyze("Test -Game").gameName shouldBe "Test -Game"
                    analyze("Test -- Game").gameName shouldBe "Test -- Game"
                }
            }
        }
    }

    fun testAnalyzeVersion(version: String) =
        analyze("Some Name [Some Metatag] [$version] More Text").version shouldBe version

    fun testAnalyzeMetaTag(metaTag: String) =
        analyze("Some name [1.2.3] [$metaTag]").metaTag shouldBe metaTag

    fun analyze(name: String) = NameHandler.analyze(name)
}