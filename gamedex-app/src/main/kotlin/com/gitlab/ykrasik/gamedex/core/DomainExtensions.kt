package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.Game

/**
 * User: ykrasik
 * Date: 17/06/2017
 * Time: 14:51
 */
fun Game.matchesSearchQuery(query: String) =
    query.isEmpty() || query.split(" ").all { word -> name.contains(word, ignoreCase = true) }