package com.gitlab.ykrasik.gamedex.provider.giantbomb.debug

import com.github.ykrasik.gamedex.common.debug.DebugCommands
import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.github.ykrasik.jaci.api.*
import com.gitlab.ykrasik.gamedex.provider.SearchResult
import com.gitlab.ykrasik.gamedex.provider.giantbomb.GiantBombClient
import com.gitlab.ykrasik.gamedex.provider.giantbomb.GiantBombDataProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Yevgeny Krasik
 */
@Singleton
@CommandPath("giantbomb")
class GiantBombDebugCommands @Inject constructor(
    private val provider: GiantBombDataProvider,
    private val client: GiantBombClient
) : DebugCommands {
    private lateinit var output: CommandOutput

    @Command
    fun search(@StringParam("name") name: String,
               @EnumParam(value = "platform", optional = true, defaultValue = "PC") platform: GamePlatform) {
        val results = provider.search(name, platform)
        results.forEach { output.message(it.toString()) }
    }

    @Command
    fun fetch(@StringParam("url") url: String) {
        val searchResult = SearchResult(
            detailUrl = url,
            name = "",
            releaseDate = null,
            score = null
        )
        val response = provider.fetch(searchResult)
        output.message(response.toString())
    }

    @CommandPath("client")
    inner class Client {

        @Command("search")
        fun search(@StringParam("name") name: String,
                   @EnumParam(value = "platform", optional = true, defaultValue = "PC") platform: GamePlatform) {
            val response = client.search(name, platform)
            output.message(response.toString())
        }

        @Command("get")
        fun get(@StringParam("detailUrl") detailUrl: String) {
            val response = client.fetch(detailUrl)
            output.message(response.toString())
        }
    }
}
