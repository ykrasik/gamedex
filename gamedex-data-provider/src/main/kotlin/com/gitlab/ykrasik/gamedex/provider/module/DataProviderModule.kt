package com.gitlab.ykrasik.gamedex.provider.module

import com.gitlab.ykrasik.gamedex.provider.DataProviderService
import com.gitlab.ykrasik.gamedex.provider.DataProviderServiceImpl
import com.google.inject.AbstractModule

/**
 * User: ykrasik
 * Date: 15/10/2016
 * Time: 15:52
 */
class DataProviderModule : AbstractModule() {
    override fun configure() {
        bind(DataProviderService::class.java).to(DataProviderServiceImpl::class.java)
    }
}