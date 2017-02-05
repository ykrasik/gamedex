package com.github.ykrasik.gamedex.common.datamodel

/**
 * User: ykrasik
 * Date: 02/01/2017
 * Time: 14:14
 */
enum class DataProviderType(val basicDataPriority: Int, val scorePriority: Int, val imagePriorty: Int) {
    Igdb(basicDataPriority = 2, scorePriority = 1, imagePriorty = 2),
    GiantBomb(basicDataPriority = 1, scorePriority = 999, imagePriorty = 1)
}