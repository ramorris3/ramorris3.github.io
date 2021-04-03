package dev.littlegames.hybrid.systems

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Pool
import dev.littlegames.hybrid.LittleGame

class EffectManager {
    private val icm: ComponentMapper<InactiveComponent> = ComponentMapper.getFor(InactiveComponent::class.java)

    private val animatedEffectPool = object : Pool<AnimatedEffect>() {
        override fun newObject(): AnimatedEffect = AnimatedEffect()
    }

    fun animatedEffect(name: String, frameDur: Float, x: Float, y: Float) {
        val e = animatedEffectPool.obtain()
        e.revive(name, frameDur, x, y)
    }

    private inner class AnimatedEffect : Entity(), Pool.Poolable, Updatable, Drawable {
        private val inactive = InactiveComponent()
        private val body = BodyComponent(this, null)
        private lateinit var animation: Animation<TextureRegion>
        private var animationStateTime = 0f

        init {
            add(inactive)
            add(body)
            add(UpdateComponent())
            add(DrawComponent(Layer.Effects))
            LittleGame.engine.addEntity(this)
        }

        fun revive(name: String, frameDur: Float, x: Float, y: Float) {
            remove(InactiveComponent::class.java)
            animation = Animation(frameDur, LittleGame.atlas.findRegions(name))
            animationStateTime = 0f
            body.position.x = x
            body.position.y = y
        }

        override fun reset() {
            add(inactive)
        }

        override fun update(delta: Float) {
            animationStateTime += delta
            if (animation.isAnimationFinished(animationStateTime)) {
                val ic = icm[this]
                if (ic == null) {
                    animatedEffectPool.free(this)
                }
            }
        }

        override fun draw() {
            val tr = animation.getKeyFrame(animationStateTime)
            LittleGame.batch.draw(
                tr,
                body.position.x - tr.regionWidth / 2f,
                body.position.y - tr.regionHeight / 2f
            )
        }
    }
}