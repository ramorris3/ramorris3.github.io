package dev.littlegames.hybrid

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Timer
import com.badlogic.gdx.utils.viewport.FitViewport
import dev.littlegames.hybrid.Constants.black
import dev.littlegames.hybrid.Constants.viewHeight
import dev.littlegames.hybrid.Constants.viewWidth
import dev.littlegames.hybrid.systems.*

class LittleGame : Game() {
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(viewWidth, viewHeight ,camera)

    companion object {
        lateinit var batch: SpriteBatch
        lateinit var atlas: TextureAtlas
        val engine = Engine()
        val effects = EffectManager()
    }

    override fun create() {
        batch = SpriteBatch()
        atlas = TextureAtlas(Gdx.files.internal("img/sprites.atlas"))
        engine.addSystem(UpdateSystem())
        engine.addSystem(HitStunSystem())
        engine.addSystem(PhysicsSystem())
        engine.addSystem(DrawSystem())
        setup()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun render() {
        Gdx.gl.glClearColor(black.r, black.g, black.b, black.a)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        camera.update()
        batch.projectionMatrix = camera.combined
        batch.begin()
        engine.update(Gdx.graphics.deltaTime)
        batch.end()

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            reset()
        }
    }

    private fun setup() {
        Player()
        Timer.schedule(object : Timer.Task() {
            override fun run() {
                val spawnX = MathUtils.random(0f, viewWidth)
                Alien(spawnX, viewHeight)
            }
        }, 0f, 3f)
    }

    private fun reset() {
        engine.removeAllEntities()
        Timer.instance().clear()
        setup()
    }

    override fun dispose() {
        batch.dispose()
        atlas.dispose()
    }
}