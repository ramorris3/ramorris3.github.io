package dev.littlegames.hybrid.systems

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem

interface Updatable {
    fun update(delta: Float)
}

class UpdateComponent : Component

class UpdateSystem : IteratingSystem(
    Family
        .all(UpdateComponent::class.java)
        .exclude(InactiveComponent::class.java, HitStunComponent::class.java)
        .get()
) {
    override fun processEntity(entity: Entity?, deltaTime: Float) {
        if (entity != null && entity is Updatable) {
            entity.update(deltaTime)
        }
    }
}