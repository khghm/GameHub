
package com.gamehub.shared.graphics

import com.gamehub.shared.graphics.architecture.*
import kotlin.test.*

// Test Components
data class PositionComponent(val x: Float, val y: Float) : Component
data class VelocityComponent(val vx: Float, val vy: Float) : Component
data class HealthComponent(val maxHealth: Int, var currentHealth: Int) : Component

class ECSWorldV2Test {

    @Test
    fun testCreateEntity() {
        val world = ECSWorldV2()
        val entity = world.createEntity()
        assertNotNull(entity)
        assertTrue(entity.id >= 0)
    }

    @Test
    fun testAddAndGetComponent() {
        val world = ECSWorldV2()
        val entity = world.createEntity()
        val testComp = PositionComponent(100f, 200f)
        world.addComponent(entity, testComp)

        val retrieved = world.getComponent(entity, PositionComponent::class)
        assertNotNull(retrieved)
        assertEquals(testComp.x, retrieved?.x)
        assertEquals(testComp.y, retrieved?.y)
    }

    @Test
    fun testHasComponent() {
        val world = ECSWorldV2()
        val entity = world.createEntity()
        assertFalse(world.hasComponent(entity, PositionComponent::class))
        world.addComponent(entity, PositionComponent(0f, 0f))
        assertTrue(world.hasComponent(entity, PositionComponent::class))
    }

    @Test
    fun testRemoveComponent() {
        val world = ECSWorldV2()
        val entity = world.createEntity()
        world.addComponent(entity, PositionComponent(0f, 0f))
        assertTrue(world.hasComponent(entity, PositionComponent::class))
        world.removeComponent(entity, PositionComponent::class)
        assertFalse(world.hasComponent(entity, PositionComponent::class))
    }

    @Test
    fun testGetEntitiesWith() {
        val world = ECSWorldV2()
        val e1 = world.createEntity()
        val e2 = world.createEntity()
        val e3 = world.createEntity()

        world.addComponent(e1, PositionComponent(1f, 1f))
        world.addComponent(e2, PositionComponent(2f, 2f))
        world.addComponent(e3, VelocityComponent(3f, 3f))

        val entitiesWithPosition = world.getEntitiesWith(PositionComponent::class)
        assertEquals(2, entitiesWithPosition.size)
    }

    @Test
    fun testQueryTwoComponents() {
        val world = ECSWorldV2()
        val e1 = world.createEntity()
        val e2 = world.createEntity()
        val e3 = world.createEntity()

        world.addComponent(e1, PositionComponent(1f, 1f))
        world.addComponent(e1, VelocityComponent(0.5f, 0.5f))
        world.addComponent(e2, PositionComponent(2f, 2f))
        world.addComponent(e3, VelocityComponent(3f, 3f))
        world.addComponent(e3, HealthComponent(100, 100))

        val queryResult = world.query(PositionComponent::class, VelocityComponent::class)
        assertEquals(1, queryResult.size)
        assertEquals(e1, queryResult[0].first)
    }

    @Test
    fun testQueryThreeComponents() {
        val world = ECSWorldV2()
        val e1 = world.createEntity()
        val e2 = world.createEntity()

        world.addComponent(e1, PositionComponent(1f, 1f))
        world.addComponent(e1, VelocityComponent(0.5f, 0.5f))
        world.addComponent(e1, HealthComponent(100, 100))
        world.addComponent(e2, PositionComponent(2f, 2f))
        world.addComponent(e2, VelocityComponent(1f, 1f))

        val queryResult = world.query(PositionComponent::class, VelocityComponent::class, HealthComponent::class)
        assertEquals(1, queryResult.size)
        assertEquals(e1, queryResult[0].first)
    }

    @Test
    fun testDestroyEntity() {
        val world = ECSWorldV2()
        val e1 = world.createEntity()
        world.addComponent(e1, PositionComponent(1f, 1f))
        assertEquals(1, world.getEntityCount())
        world.destroyEntity(e1)
        assertEquals(0, world.getEntityCount())
        assertNull(world.getComponent(e1, PositionComponent::class))
    }

    @Test
    fun testEntityReuse() {
        val world = ECSWorldV2()
        val e1 = world.createEntity()
        val e1Id = e1.id
        world.destroyEntity(e1)
        val e2 = world.createEntity()
        assertEquals(e1Id, e2.id) // Should reuse the ID
    }
}
