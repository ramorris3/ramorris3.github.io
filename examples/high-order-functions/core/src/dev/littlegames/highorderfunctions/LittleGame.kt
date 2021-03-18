package dev.littlegames.highorderfunctions

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import dev.littlegames.highorderfunctions.Constants.black
import dev.littlegames.highorderfunctions.Constants.viewHeight
import dev.littlegames.highorderfunctions.Constants.viewWidth
import dev.littlegames.highorderfunctions.Constants.white

class LittleGame : Game() {
    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: FitViewport
    private lateinit var board: Board
    private lateinit var titleLabel: Label
    private lateinit var infoLabel: Label
    private lateinit var scoreLabel: Label
    private lateinit var bestScoreLabel: Label
    var bestScore: Int = 0
        set(value) {
            bestScoreLabel.setText("Best: $value")
            bestScoreLabel.setPosition(
                viewWidth - bestScoreLabel.width - 16f,
                viewHeight - titleLabel.height - infoLabel.height - scoreLabel.height
            )
            bestScoreLabel.setFontScale(1.5f)
            field = value
        }
    var score: Int = 0
        set(value) {
            scoreLabel.setText("Score: $value")
            scoreLabel.setFontScale(1.5f)
            if (value > bestScore) {
                bestScore = value
            }
            field = value
        }

    companion object {
        lateinit var batch: SpriteBatch
        lateinit var fonts: Fonts
        lateinit var assets: AssetManager
    }

    override fun create() {
        batch = SpriteBatch()
        fonts = Fonts()
        assets = AssetManager()
        assets.load("tile.png", Texture::class.java)
        assets.load("star.png", Texture::class.java)
        assets.finishLoading()

        camera = OrthographicCamera()
        viewport = FitViewport(viewWidth, viewHeight, camera)

        board = Board(this)
        Gdx.input.inputProcessor = board

        val titleStyle = Label.LabelStyle(fonts.titleFont, white)
        titleLabel = Label("Stars", titleStyle)
        titleLabel.setAlignment(Align.topLeft, Align.topLeft)
        titleLabel.setPosition(16f, viewHeight - titleLabel.height)
        val defaultStyle = Label.LabelStyle(fonts.defaultFont, white)
        infoLabel = Label("Combine matching tiles", defaultStyle)
        infoLabel.setPosition(16f, viewHeight - titleLabel.height - infoLabel.height)
        scoreLabel = Label("Score: $score", defaultStyle)
        val scoreY = viewHeight - titleLabel.height - infoLabel.height - scoreLabel.height
        scoreLabel.setPosition(16f, scoreY)
        bestScoreLabel = Label("Best: $bestScore", defaultStyle)
        bestScoreLabel.setAlignment(Align.right)
        bestScoreLabel.layout()
        bestScoreLabel.setPosition(viewWidth - bestScoreLabel.width - 16f, scoreY)
    }

    override fun render() {
        val delta = Gdx.graphics.deltaTime
        Gdx.gl.glClearColor(black.r, black.g, black.b, black.a)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        camera.update()
        batch.projectionMatrix = camera.combined
        batch.begin()
        board.update(delta)
        board.draw()
        titleLabel.draw(batch, 1f)
        infoLabel.draw(batch, 1f)
        if (scoreLabel.fontScaleX > 1f) {
            scoreLabel.setFontScale(approach(scoreLabel.fontScaleX, 1f, 10f * delta))
        }
        scoreLabel.draw(batch, 1f)
        if (bestScoreLabel.fontScaleX > 1f) {
            bestScoreLabel.setFontScale(approach(bestScoreLabel.fontScaleX, 1f, 10f * delta))
        }
        bestScoreLabel.draw(batch, 1f)
        batch.end()
    }

    fun gameOver() {
        infoLabel.setText("Game over! (R to restart)")
    }

    fun reset() {
        infoLabel.setText("Combine matching tiles")
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun dispose() {
        batch.dispose()
        assets.dispose()
        fonts.dispose()
    }
}