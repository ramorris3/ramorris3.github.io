package dev.littlegames.hybrid.systems

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.SortedIteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import dev.littlegames.hybrid.Constants.white
import dev.littlegames.hybrid.LittleGame

enum class Layer {
    Bullets,
    Entities,
    Effects,
}

interface Drawable {
    fun draw()
}

class DrawComponent(val layer: Layer) : Component

class DrawSystem : SortedIteratingSystem(
    Family.all(DrawComponent::class.java)
        .exclude(InactiveComponent::class.java)
        .get(),
    DrawComparator()
) {
    private val flashShader: ShaderProgram
    private val hscm: ComponentMapper<HitStunComponent> = ComponentMapper.getFor(HitStunComponent::class.java)

    init {
        ShaderProgram.pedantic = false
        flashShader = ShaderProgram(
            Gdx.files.internal("shaders/flash.vert"),
            Gdx.files.internal("shaders/flash.frag")
        )
        if (!flashShader.isCompiled) {
            System.err.println(flashShader.log)
            System.exit(0)
        }
    }

    override fun processEntity(entity: Entity?, deltaTime: Float) {
        if (entity != null && entity is Drawable) {
            val hitStunComp = hscm[entity]
            var previousBatchColor: Color? = null
            if (hitStunComp != null) {
                // flash white if hit-stunned
                previousBatchColor = Color(LittleGame.batch.color)
                LittleGame.batch.shader = flashShader
                LittleGame.batch.color = white

            }
            entity.draw()
            if (hitStunComp != null) {
                // reset shader and color
                LittleGame.batch.shader = null
                LittleGame.batch.color = previousBatchColor
            }
        }
    }

    private class DrawComparator : Comparator<Entity> {
        private val dcm: ComponentMapper<DrawComponent> = ComponentMapper.getFor(DrawComponent::class.java)
        override fun compare(e1: Entity?, e2: Entity?): Int {
            if (e1 != null && e2 != null && e1 is Drawable && e2 is Drawable) {
                val dc1 = dcm.get(e1)
                val dc2 = dcm.get(e2)
                return dc1.layer.compareTo(dc2.layer)
            }
            return 0
        }
    }
}
