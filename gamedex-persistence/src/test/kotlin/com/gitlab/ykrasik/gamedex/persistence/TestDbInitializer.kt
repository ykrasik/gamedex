package com.gitlab.ykrasik.gamedex.persistence

/**
 * User: ykrasik
 * Date: 06/10/2016
 * Time: 21:18
 */
object TestDbInitializer {
    private val initializer: DbInitializer = DbInitializer(PersistenceConfig.test)

    init {
        initializer.connect()
        initializer.enableDestroy()
    }

    fun init() {
        initializer.migrate()
    }

    fun destroy() {
        initializer.destroy()
    }

    fun reload() {
        destroy()
        init()
    }
}