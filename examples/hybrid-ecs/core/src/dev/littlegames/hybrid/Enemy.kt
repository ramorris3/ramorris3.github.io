package dev.littlegames.hybrid

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.utils.Timer
import dev.littlegames.hybrid.Constants.viewWidth
import dev.littlegames.hybrid.systems.*

abstract class Enemy(name: String, x: Float, y: Float, w: Float, h: Float, private var hp: Int) : Entity(), Updatable, Drawable {
    protected val body = BodyComponent(this, BodyType.Enemy)
    private val animation = Animation(
        0.1f,
        LittleGame.atlas.findRegions(name),
        Animation.PlayMode.LOOP
    )
    private var animationStateTime = 0f

    init {
        body.size.set(w, h)
        body.position.set(x, y)
        add(body)
        add(UpdateComponent())
        add(DrawComponent(Layer.Entities))
        LittleGame.engine.addEntity(this)
    }

    override fun update(delta: Float) {
        animationStateTime += delta
        if (body.position.y < -body.size.y
            || body.position.x < -body.size.x
            || body.position.x == viewWidth
        ) {
            LittleGame.engine.removeEntity(this)
        }
    }

    override fun draw() {
        val img = animation.getKeyFrame(animationStateTime)
        LittleGame.batch.draw(
            img,
            body.center.x - img.regionWidth / 2f,
            body.center.y - img.regionHeight / 2f
        )
    }

    fun onShot() {
        hp -= 1
        if (hp <= 0) {
            die()
        } else {
            body.position.y += 5
            add(HitStunComponent())
        }
    }

    open fun die() {
        LittleGame.effects.animatedEffect("explosion", 0.09f, body.center.x, body.center.y)
        LittleGame.engine.removeEntity(this)
    }
}

class Alien(x: Float, y: Float) : Enemy("enemy", x, y, 48f, 48f, 10) {
    private val bombInterval = 2.75f
    private val bombTask: Timer.Task

    init {
        body.velocity.y = -50f
        bombTask = Timer.schedule(object: Timer.Task() {
            override fun run() {
                (-30..30 step 30).forEach { angle ->
                    val bombX = body.center.x - 12f
                    val bombY = body.bottom
                    Bomb(bombX, bombY, angle.toFloat())
                }
            }
        }, 0f, bombInterval)
    }

    override fun die() {
        super.die()
        bombTask.cancel()
    }
}

class Bomb(x: Float, y: Float, rotation: Float) : Enemy("enemy-bomb", x, y, 24f, 24f, 3) {
    init {
        body.velocity.y = -85f
        body.velocity.rotateDeg(rotation)
    }
}