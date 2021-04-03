package dev.littlegames.hybrid

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g2d.Animation
import dev.littlegames.hybrid.Constants.viewWidth
import dev.littlegames.hybrid.systems.*

class Player : Entity(), Updatable, Drawable {
    private val body = BodyComponent(this, BodyType.Player)
    private val animation = Animation(
        0.04f,
        LittleGame.atlas.findRegions("player"),
        Animation.PlayMode.LOOP
    )
    private var animationStateTime = 0f
    private val bullets = PlayerBullets()
    private val bulletInterval = 0.1f
    private var bulletCountdown = 0f

    init {
        body.size.set(16f, 32f)
        body.position.set(viewWidth / 2f - body.size.x / 2f,  16f)
        body.collisionHandlers[BodyType.Enemy] = { enemy ->
            enemy as Enemy
            enemy.die()
            die()
        }
        add(body)
        add(UpdateComponent())
        add(DrawComponent(Layer.Entities))
        LittleGame.engine.addEntity(this)
    }

    override fun update(delta: Float) {
        animationStateTime += delta
        val speed = 150f
        body.velocity.x = when {
            Gdx.input.isKeyPressed(Input.Keys.LEFT) -> -speed
            Gdx.input.isKeyPressed(Input.Keys.RIGHT) -> speed
            else -> 0f
        }
        body.velocity.y = when {
            Gdx.input.isKeyPressed(Input.Keys.DOWN) -> -speed
            Gdx.input.isKeyPressed(Input.Keys.UP) -> speed
            else -> 0f
        }

        if (body.center.x > viewWidth) {
            body.position.x -= viewWidth
        }
        if (body.center.x < 0) {
            body.position.x += viewWidth
        }
        body.position.y = body.position.y.coerceIn(0f, Constants.viewHeight - body.size.y)

        if (bulletCountdown > 0f) {
            bulletCountdown -= delta
        } else if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            bulletCountdown = bulletInterval
            bullets.fire(body.center.x - 8f, body.center.y)
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

    private fun die() {
        LittleGame.effects.animatedEffect("explosion", 0.09f, body.center.x, body.center.y)
        LittleGame.engine.removeEntity(this)
    }
}