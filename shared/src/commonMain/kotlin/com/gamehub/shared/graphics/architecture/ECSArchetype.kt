
package com.gamehub.shared.graphics.architecture

import kotlin.reflect.KClass

// ==================== Archetype ====================
data class Archetype(
    val componentTypes: Set<KClass<out Component>>,
    val entities: MutableList<Entity> = mutableListOf(),
    val componentData: MutableMap<KClass<out Component>, MutableList<Component>> = mutableMapOf()
) {
    fun addEntity(entity: Entity, components: Map<KClass<out Component>, Component>) {
        entities.add(entity)
        components.forEach { (type, comp) ->
            componentData.getOrPut(type) { mutableListOf() }.add(comp)
        }
    }

    fun removeEntity(entity: Entity): Int? {
        val index = entities.indexOf(entity)
        if (index == -1) return null
        entities.removeAt(index)
        componentData.values.forEach { it.removeAt(index) }
        return index
    }

    fun <T : Component> getComponents(type: KClass<T>): List<T> {
        return componentData[type]?.mapNotNull { it as? T } ?: emptyList()
    }
}

// ==================== Improved ECS World ====================
class ECSWorldV2 {
    private val entityArchetypes = mutableMapOf<Entity, Archetype>()
    private val archetypes = mutableMapOf<Set<KClass<out Component>>, Archetype>()
    private val entityComponents = mutableMapOf<Entity, MutableMap<KClass<out Component>, Component>>()
    private var nextId = 0
    private val removedEntities = mutableListOf<Entity>() // for reuse

    // ==================== Entity Management ====================
    fun createEntity(): Entity {
        return if (removedEntities.isNotEmpty()) {
            removedEntities.removeAt(removedEntities.size - 1)
        } else {
            Entity(nextId++)
        }
    }

    fun destroyEntity(entity: Entity) {
        entityArchetypes[entity]?.removeEntity(entity)
        entityArchetypes.remove(entity)
        entityComponents.remove(entity)
        removedEntities.add(entity)
    }

    fun getEntityCount(): Int = entityComponents.size

    // ==================== Component Management ====================
    fun <T : Component> addComponent(entity: Entity, component: T) {
        val currentComponents = entityComponents.getOrPut(entity) { mutableMapOf() }
        val oldTypes = currentComponents.keys.toSet()
        currentComponents[component.componentType] = component
        val newTypes = currentComponents.keys.toSet()

        // Move to new archetype
        entityArchetypes[entity]?.removeEntity(entity)
        val newArchetype = archetypes.getOrPut(newTypes) { Archetype(newTypes) }
        newArchetype.addEntity(entity, currentComponents)
        entityArchetypes[entity] = newArchetype
    }

    fun <T : Component> removeComponent(entity: Entity, type: KClass<T>) {
        val currentComponents = entityComponents[entity] ?: return
        val oldTypes = currentComponents.keys.toSet()
        currentComponents.remove(type)
        val newTypes = currentComponents.keys.toSet()

        // Move to new archetype
        entityArchetypes[entity]?.removeEntity(entity)
        if (newTypes.isNotEmpty()) {
            val newArchetype = archetypes.getOrPut(newTypes) { Archetype(newTypes) }
            newArchetype.addEntity(entity, currentComponents)
            entityArchetypes[entity] = newArchetype
        } else {
            entityArchetypes.remove(entity)
        }
    }

    fun <T : Component> getComponent(entity: Entity, type: KClass<T>): T? {
        return entityComponents[entity]?.get(type) as? T
    }

    fun <T : Component> hasComponent(entity: Entity, type: KClass<T>): Boolean {
        return entityComponents[entity]?.containsKey(type) ?: false
    }

    // ==================== Queries ====================
    fun <T : Component> getEntitiesWith(type: KClass<T>): List<Pair<Entity, T>> {
        val result = mutableListOf<Pair<Entity, T>>()
        archetypes
            .filter { it.key.contains(type) }
            .forEach { (_, archetype) ->
                val components = archetype.getComponents(type)
                archetype.entities.zip(components).forEach { (entity, comp) ->
                    result.add(entity to comp)
                }
            }
        return result
    }

    // Query with 2 components
    fun <A : Component, B : Component> query(
        typeA: KClass<A>,
        typeB: KClass<B>
    ): List<Triple<Entity, A, B>> {
        val result = mutableListOf<Triple<Entity, A, B>>()
        val requiredTypes = setOf(typeA, typeB)
        archetypes
            .filter { it.key.containsAll(requiredTypes) }
            .forEach { (_, archetype) ->
                val aList = archetype.getComponents(typeA)
                val bList = archetype.getComponents(typeB)
                archetype.entities.zip(aList).zip(bList).forEach { (pair, b) ->
                    result.add(Triple(pair.first, pair.second, b))
                }
            }
        return result
    }

    // Query with 3 components
    fun <A : Component, B : Component, C : Component> query(
        typeA: KClass<A>,
        typeB: KClass<B>,
        typeC: KClass<C>
    ): List<Quad<Entity, A, B, C>> {
        val result = mutableListOf<Quad<Entity, A, B, C>>()
        val requiredTypes = setOf(typeA, typeB, typeC)
        archetypes
            .filter { it.key.containsAll(requiredTypes) }
            .forEach { (_, archetype) ->
                val aList = archetype.getComponents(typeA)
                val bList = archetype.getComponents(typeB)
                val cList = archetype.getComponents(typeC)
                archetype.entities.zip(aList).zip(bList).zip(cList).forEach { (triple, c) ->
                    result.add(Quad(triple.first.first, triple.first.second, triple.second, c))
                }
            }
        return result
    }

    // ==================== Helper Classes ====================
    data class Quad<out A, out B, out C, out D>(val first: A, val second: B, val third: C, val fourth: D)
}
