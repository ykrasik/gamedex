package com.gitlab.ykrasik.gamedex.provider.giantbomb.debug

import com.github.ykrasik.jaci.api.*
import com.gitlab.ykrasik.gamedex.common.DebugCommands
import com.gitlab.ykrasik.gamedex.common.datamodel.GamePlatform
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.provider.giantbomb.GiantBombDataProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Yevgeny Krasik
 */
@Singleton
@CommandPath("giantbomb")
class GiantBombDebugCommands @Inject constructor(
    private val provider: GiantBombDataProvider
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
        val searchResult = ProviderSearchResult(
            apiUrl = url,
            name = "",
            releaseDate = null,
            score = null,
            thumbnailUrl = null
        )
        val response = provider.fetch(searchResult)
        output.message(response.toString())
    }
}
