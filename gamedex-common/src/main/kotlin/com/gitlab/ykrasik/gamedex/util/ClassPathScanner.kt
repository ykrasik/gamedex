package com.gitlab.ykrasik.gamedex.util

import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner
import java.net.URL
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 07/10/2016
 * Time: 16:23
 */
object ClassPathScanner {
    fun scanResources(basePackage: String, predicate: (String) -> Boolean): List<URL> {
        return Reflections(basePackage, ResourcesScanner()).getResources { predicate(it!!) }
            .map { this::class.java.getResource("/" + it) }
    }

    fun <T : Any> scanSubTypes(basePackage: String, type: KClass<T>): Set<Class<out T>> =
        Reflections(basePackage).getSubTypesOf(type.java)
}