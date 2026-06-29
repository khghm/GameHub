
package com.gamehub.shared.graphics.architecture

import kotlin.reflect.KClass

interface Component {
    val componentType: KClass<out Component> get() = this::class
}

class Entity(val id: Int)

@Deprecated("Use ECSWorldV2 for better performance")
class ECSWorld {
    private var nextId = 0
    private val entities = mutableListOf<Entity>()
    private val componentStore = mutableMapOf<KClass<out Component>, MutableMap<Int, Component>>()

    fun createEntity(): Entity {
        val id = nextId++
        val entity = Entity(id)
        entities.add(entity)
        return entity
    }

    fun destroyEntity(entity: Entity) {
        entities.remove(entity)
        componentStore.values.forEach { it.remove(entity.id) }
    }

    fun <T : Component> addComponent(entity: Entity, component: T) {
        componentStore.getOrPut(component.componentType) { mutableMapOf() }[entity.id] = component
    }

    fun <T : Component> removeComponent(entity: Entity, type: KClass<T>) {
        componentStore[type]?.remove(entity.id)
    }

    fun <T : Component> getEntitiesWith(type: KClass<T>): List<Pair<Entity, T>> {
        val components = componentStore[type] ?: return emptyList()
        return entities.mapNotNull { entity ->
            val comp = components[entity.id] as? T ?: return@mapNotNull null
            entity to comp
        }
    }

    fun <T : Component> getComponent(entity: Entity, type: KClass<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return componentStore[type]?.get(entity.id) as? T
    }

    fun <T : Component> hasComponent(entity: Entity, type: KClass<T>): Boolean {
        return componentStore[type]?.containsKey(entity.id) ?: false
    }

    fun <A : Component, B : Component> query(
        typeA: KClass<A>, typeB: KClass<B>
    ): List<Triple<Entity, A, B>> {
        return getEntitiesWith(typeA).mapNotNull { (e, a) ->
            val b = getComponent(e, typeB) ?: return@mapNotNull null
            Triple(e, a, b)
        }
    }

    fun getEntityCount(): Int = entities.size
}

@Deprecated("Use ECSSystemV2 with ECSWorldV2")
interface ECSSystem {
    fun update(world: ECSWorld, deltaMs: Long)
}

interface ECSSystemV2 {
    fun update(world: ECSWorldV2, deltaMs: Long)
}
