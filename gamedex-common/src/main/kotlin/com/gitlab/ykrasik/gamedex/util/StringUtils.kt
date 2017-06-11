package com.gitlab.ykrasik.gamedex.util

import com.google.common.net.UrlEscapers

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 20:23
 */
fun String.urlEncoded() = UrlEscapers.urlFragmentEscaper().escape(this)