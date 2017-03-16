package com.gitlab.ykrasik.gamedex.util

import com.gitlab.ykrasik.gamedex.core.Notification
import kotlinx.coroutines.experimental.Job

/**
 * User: ykrasik
 * Date: 17/03/2017
 * Time: 15:52
 */
data class NotifiableJob<out J : Job>(
    val job: J,
    val notification: Notification
)

fun <J : Job> notifiableJob(f: (Notification) -> J): NotifiableJob<J> {
    val notification = Notification()
    val job = f(notification)
    return NotifiableJob(job, notification)
}