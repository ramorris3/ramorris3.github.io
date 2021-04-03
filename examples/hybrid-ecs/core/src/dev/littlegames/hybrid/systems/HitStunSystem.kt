package dev.littlegames.hybrid.systems

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem


class HitStunComponent(var remainingTime: Float = 0.25f) : Component

class HitStunSystem : IteratingSystem(Family.all(HitStunComponent::class.java).get()) {
    private val hscm: ComponentMapper<HitStunComponent> = ComponentMapper.getFor(HitStunComponent::class.java)

    override fun processEntity(entity: Entity?, deltaTime: Float) {
        if (entity != null) {
            val comp = hscm[entity]
            comp.remainingTime -= deltaTime
            if (comp.remainingTime <= 0f) {
                entity.remove(HitStunComponent::class.java)
            }
        }
    }
}