package dev.littlegames.hybrid

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import dev.littlegames.hybrid.Constants.viewHeight
import dev.littlegames.hybrid.systems.*

class PlayerBullets {
    private val icm: ComponentMapper<InactiveComponent> = ComponentMapper.getFor(InactiveComponent::class.java)

    private val bulletPool = object : Pool<PlayerBullet>() {
        override fun newObject(): PlayerBullet = PlayerBullet()
    }

    fun fire(x: Float, y: Float) {
        val b = bulletPool.obtain()
        b.revive(x, y)
    }

    private inner class PlayerBullet : Entity(), Pool.Poolable, Updatable, Drawable {
        private val inactive = InactiveComponent()
        private val body = BodyComponent(this, BodyType.PlayerBullet)
        private val img = LittleGame.atlas.findRegion("player-bullet")
        private val lifespan = 0.8f
        private var lifespanCountdown = lifespan

        init {
            add(inactive)
            body.size.set(16f, 24f)
            body.collisionHandlers[BodyType.Enemy] = { enemy ->
                val ic = icm[this]
                if (ic == null) {
                    free()
                    LittleGame.effects.animatedEffect("bullet-hit", 0.04f, body.center.x, body.center.y)
                    (enemy as Enemy).onShot()
                }
            }
            add(body)
            add(UpdateComponent())
            add(DrawComponent(Layer.Bullets))
            LittleGame.engine.addEntity(this)
        }

        fun revive(x: Float, y: Float) {
            remove(InactiveComponent::class.java)
            body.position.x = x
            body.position.y = y
            body.velocity.y = 400f
            lifespanCountdown = lifespan
        }

        private fun free() {
            val ic = icm[this]
            if (ic == null) {
                bulletPool.free(this)
            }
        }

        override fun reset() {
            add(inactive)
        }

        override fun update(delta: Float) {
            lifespanCountdown -= delta
            if (lifespanCountdown <= 0f || body.position.y > viewHeight) {
                LittleGame.effects.animatedEffect(
                    "player-bullet-expire",
                    0.01f,
                    body.center.x,
                    body.center.y
                )
                free()
            }
        }

        override fun draw() {
            LittleGame.batch.draw(
                img,
                body.center.x - img.regionWidth / 2f,
                body.center.y - img.regionHeight / 2f
            )
        }
    }
}