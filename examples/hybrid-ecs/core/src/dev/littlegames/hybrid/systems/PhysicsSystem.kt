package dev.littlegames.hybrid.systems

import com.badlogic.ashley.core.*
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import dev.littlegames.hybrid.Constants.viewHeight
import dev.littlegames.hybrid.Constants.viewWidth
import dev.littlegames.hybrid.LittleGame

enum class BodyType {
    Player,
    PlayerBullet,
    Enemy,
}

class BodyComponent(val entity: Entity, val type: BodyType?) : Component {
    val size = Vector2()
    val position = Vector2()
    val velocity = Vector2()

    val left: Float
    get() = position.x
    val right: Float
    get() = position.x + size.x
    val bottom: Float
    get() = position.y
    val top: Float
    get() = position.y + size.y
    val center: Vector2
    get() = Vector2(position.x + size.x / 2f, position.y + size.y / 2f)

    val collisionHandlers = HashMap<BodyType, (other: Entity) -> Unit>()

    fun isOverlapping(other: BodyComponent) : Boolean {
        val r1 = Rectangle(position.x, position.y, size.x, size.y)
        val r2 = Rectangle(other.position.x, other.position.y, other.size.x, other.size.y)
        return !equals(other) && r1.overlaps(r2)
    }
}

private class World {
    private val collisionGroups = HashMap<BodyType, ArrayList<BodyComponent>>()

    init {
        for (type in BodyType.values()) {
            collisionGroups[type] = arrayListOf()
        }
    }

    fun add(body: BodyComponent) {
        if (body.type != null) {
            collisionGroups[body.type]?.add(body)
        }
    }

    fun remove(body: BodyComponent) {
        if (body.type != null) {
            collisionGroups[body.type]?.remove(body)
        } else {
            BodyType.values().forEach { collisionGroups[it]?.remove(body) }
        }
    }

    fun getOverlapping(body: BodyComponent, type: BodyType) : BodyComponent? {
        return collisionGroups[type]?.firstOrNull { it.isOverlapping(body) }
    }
}

class PhysicsSystem : IteratingSystem(
    Family
        .all(BodyComponent::class.java)
        .exclude(InactiveComponent::class.java, HitStunComponent::class.java)
        .get()
){
    private val world = World()  // in a world with more sophisticated physics this could be a Box2D world
    private val bcm: ComponentMapper<BodyComponent> = ComponentMapper.getFor(BodyComponent::class.java)

    init {
        val bodiesFamily = Family.all(BodyComponent::class.java).get()
        val worldEntityListener = object : EntityListener {
            override fun entityAdded(entity: Entity?) {
                if (entity != null) {
                    val body = bcm[entity]
                    world.add(body)
                }
            }

            override fun entityRemoved(entity: Entity?) {
                if (entity != null) {
                    val body = bcm[entity]
                    world.remove(body)
                }
            }
        }
        LittleGame.engine.addEntityListener(bodiesFamily, worldEntityListener)
    }

    override fun processEntity(entity: Entity?, deltaTime: Float) {
        if (entity != null) {
            val body = bcm[entity]
            body.position.x += body.velocity.x * deltaTime
            body.position.y += body.velocity.y * deltaTime
            body.collisionHandlers.forEach { (type, collisionHandler) ->
                val other = world.getOverlapping(body, type)
                if (other != null) {
                    collisionHandler.invoke(other.entity)
                }
            }
        }
    }
}