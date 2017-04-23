package com.gitlab.ykrasik.gamedex.preferences

/**
 * User: ykrasik
 * Date: 23/04/2017
 * Time: 12:49
 */
// TODO: I don't like displayName on the enum.
enum class GameSort constructor(val displayName: String) {
    nameAsc("Name \u2191"),
    nameDesc("Name \u2193"),
    criticScoreAsc("Critic Score \u2191"),
    criticScoreDesc("Critic Score \u2193"),
    userScoreAsc("User Score \u2191"),
    userScoreDesc("User Score \u2193"),
    minScoreAsc("Min Score \u2191"),
    minScoreDesc("Min Score \u2193"),
    avgScoreAsc("Average Score \u2191"),
    avgScoreDesc("Average Score \u2193"),
    releaseDateAsc("Release Date \u2191"),
    releaseDateDesc("Release Date \u2193"),
    dateAddedAsc("Date Added \u2191"),
    dateAddedDesc("Date Added \u2193");

    override fun toString() = displayName
    
    private companion object {
        final val downArrow = "\u2191"
    }
}