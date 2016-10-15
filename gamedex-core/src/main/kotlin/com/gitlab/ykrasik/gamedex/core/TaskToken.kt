package com.gitlab.ykrasik.gamedex.core

/**
 * User: ykrasik
 * Date: 13/10/2016
 * Time: 18:12
 */
abstract class TaskToken(private val name: String) : Runnable {
    private val messageListeners = mutableListOf<(String) -> Unit>()
    private val progressListeners = mutableListOf<(Double) -> Unit>()

    // TODO: Needed?
    private var started = false
    private var stopped = false

//    override fun run() {
//        check(!started) { "Task '$name' has already been completed!" }
//        started = true
//        runnable()
//    }

    fun messageListener(f: (String) -> Unit) {
        messageListeners += f
    }

    fun progressListener(f: (Double) -> Unit) {
        progressListeners += f
    }

    protected fun message(msg: String): Unit = messageListeners.forEach { it(msg) }

    protected fun progress(current: Int, total: Int): Unit = progress(current.toDouble() / total)
    protected fun progress(progress: Double): Unit = progressListeners.forEach { it(progress) }

    fun stop() {
        stopped = true
    }

    fun isStopped(): Boolean = stopped || Thread.interrupted()

    override fun toString() = name
}