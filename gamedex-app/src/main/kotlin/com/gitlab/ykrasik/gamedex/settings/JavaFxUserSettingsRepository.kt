//package com.gitlab.ykrasik.gamedex.settings
//
//import com.fasterxml.jackson.databind.JsonNode
//import com.fasterxml.jackson.databind.node.ObjectNode
//import com.gitlab.ykrasik.gamedex.UserSettingsRepo
//import com.gitlab.ykrasik.gamedex.UserSettingsRepoFactory
//import com.gitlab.ykrasik.gamedex.util.create
//import com.gitlab.ykrasik.gamedex.util.objectMapper
//import com.gitlab.ykrasik.gamedex.util.toFile
//import javafx.beans.property.*
//import kotlinx.coroutines.experimental.asCoroutineDispatcher
//import kotlinx.coroutines.experimental.launch
//import tornadofx.onChange
//import java.util.concurrent.Executors
//import javax.inject.Singleton
//import kotlin.reflect.KClass
//
///**
// * User: ykrasik
// * Date: 03/03/2018
// * Time: 17:36
// */
//@Singleton
//class FileBasedUserSettingsRepoFactory : UserSettingsRepoFactory {
//    private val cache = mutableMapOf<String, FileBasedUserSettingsRepository>()
//    override fun forNamespace(namespace: String) = cache.getOrPut(namespace) { FileBasedUserSettingsRepository(namespace) }
//}
//
//class FileBasedUserSettingsRepository(name: String) : UserSettingsRepo {
//    private val file = "conf/$name.json".toFile()
//    private var changeListeners = mutableListOf<() -> Unit>()
//
//    private val settings: ObjectNode =
//        if (file.exists()) {
//            try {
//                objectMapper.readTree(file) as ObjectNode
//            } catch (e: Exception) {
//                objectMapper.valueToTree<ObjectNode>(emptyMap<Any, Any>())
//            }
//        } else {
//            objectMapper.valueToTree(emptyMap<Any, Any>())
//        }
//
//    override fun <T : Any> get(name: String, klass: KClass<T>): T? = withNode(name) { node ->
//        objectMapper.convertValue(node, klass.java)
//    }
//
//    override fun <K : Any, V : Any> getMap(name: String, keyClass: KClass<K>, valueClass: KClass<V>): Map<K, V>? = withNode(name) { node ->
//        val type = objectMapper.typeFactory.constructMapType(Map::class.java, keyClass.java, valueClass.java)
//        objectMapper.convertValue<Map<K, V>>(node, type)
//    }
//
//    override fun <T : Any> getList(name: String, klass: KClass<T>): List<T>? = withNode(name) { node ->
//        val type = objectMapper.typeFactory.constructCollectionType(List::class.java, klass.java)
//        objectMapper.convertValue<List<T>>(node, type)
//    }
//
//    private fun <T> withNode(name: String, f: (JsonNode) -> T): T? {
//        val node = settings[name] ?: return null
//        return f(node)
//    }
//
//    override fun set(name: String, value: Any?) {
//        settings.set(name, objectMapper.valueToTree(value))
//        onChange()
//    }
//
//    private fun onChange() {
//        changeListeners.forEach { it() }
//        launch(dispatcher) {
//            file.create()
//            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, settings)
//        }
//    }
//
//    override fun onChange(f: () -> Unit) {
//        changeListeners.add(f)
//    }
//
//    private companion object {
//        private val dispatcher = Executors.newSingleThreadScheduledExecutor {
//            Thread(it, "settingsWriter").apply { isDaemon = true }
//        }.asCoroutineDispatcher()
//    }
//}
//
//inline fun <reified T : Any> UserSettingsRepo.property(name: String, default: T?): ObjectProperty<T> = property0(name, default) { SimpleObjectProperty(it) }
//inline fun <reified K : Any, reified V : Any> UserSettingsRepo.mapProperty(name: String, default: Map<K, V>): ObjectProperty<Map<K, V>> = mapProperty0(name, default)
//inline fun <reified T : Any> UserSettingsRepo.listProperty(name: String, default: List<T>): ObjectProperty<List<T>> = listProperty0(name, default)
//fun UserSettingsRepo.property(name: String, default: Boolean): BooleanProperty = property0(name, default) { SimpleBooleanProperty(it!!) }
//fun UserSettingsRepo.property(name: String, default: Int): IntegerProperty = property0(name, default) { SimpleIntegerProperty(it!!) }
//fun UserSettingsRepo.property(name: String, default: Double): DoubleProperty = property0(name, default) { SimpleDoubleProperty(it!!) }
//fun UserSettingsRepo.property(name: String, default: String): StringProperty = property0(name, default) { SimpleStringProperty(it) }
//
//inline fun <R : Property<in T>, reified T : Any> UserSettingsRepo.property0(name: String, default: T?, factory: (T?) -> R): R {
//    val value = get(name, T::class) ?: default.apply { set(name, default) }
//    val property = factory(value)
//    property.onChange {
//        set(name, it)
//    }
//    return property
//}
//
//inline fun <reified K : Any, reified V : Any> UserSettingsRepo.mapProperty0(name: String, default: Map<K, V>): ObjectProperty<Map<K, V>> {
//    val value = getMap(name, K::class, V::class) ?: default.apply { set(name, default) }
//    val property = SimpleObjectProperty(value)
//    property.onChange {
//        set(name, it)
//    }
//    return property
//}
//
//inline fun <reified T : Any> UserSettingsRepo.listProperty0(name: String, default: List<T>): ObjectProperty<List<T>> {
//    val value = getList(name, T::class) ?: default.apply { set(name, default) }
//    val property = SimpleObjectProperty(value)
//    property.onChange {
//        set(name, it)
//    }
//    return property
//}