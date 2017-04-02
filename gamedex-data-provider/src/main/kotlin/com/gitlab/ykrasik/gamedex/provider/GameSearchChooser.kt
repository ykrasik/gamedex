package com.gitlab.ykrasik.gamedex.provider

import java.io.File

/**
 * User: ykrasik
 * Date: 30/12/2016
 * Time: 22:01
 */
data class ChooseSearchResultData(
    val name: String,
    val path: File,
    val info: DataProviderInfo,
    val searchResults: List<ProviderSearchResult>,
    val canProceedWithout: Boolean
)

sealed class SearchResultChoice {
    data class Ok(val result: ProviderSearchResult) : SearchResultChoice()
    data class NewSearch(val newSearch: String) : SearchResultChoice()
    object Cancel : SearchResultChoice()
    object ProceedWithout : SearchResultChoice()
}