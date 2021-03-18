package dev.littlegames.highorderfunctions

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import dev.littlegames.highorderfunctions.Constants.symbolHeight
import dev.littlegames.highorderfunctions.Constants.symbolWidth
import dev.littlegames.highorderfunctions.Constants.tileHeight
import dev.littlegames.highorderfunctions.Constants.tilePad
import dev.littlegames.highorderfunctions.Constants.tileWidth
import dev.littlegames.highorderfunctions.Constants.white

enum class TileType {
    ONE,
    TWO,
    THREE,
    FOUR,
    STAR;

    companion object {
        fun random(): TileType {
            val i = MathUtils.random(values().size - 2) // no stars
            return values()[i]
        }
    }

    fun next() : TileType {
        val i = (ordinal + 1).coerceAtMost(values().size - 1)
        return values()[i]
    }
}

class Tile(private val board: Board, startTx: Int, startTy: Int) {
    var type = TileType.random()
    private val label: Label
    private val tileImg = TextureRegion(LittleGame.assets["tile.png", Texture::class.java])
    private var starImg = TextureRegion(LittleGame.assets["star.png", Texture::class.java])

    var tx: Int = startTx
    private var drawX: Float = tx * (tileWidth + tilePad * 2f) + tilePad
    var ty: Int = startTy
    private var drawY: Float = ty * (tileHeight + tilePad * 2f) + tilePad
    private val slideSpeed = 5000f

    private var scale = 0f  // start a 0 so tiles "grow in"
    private val scaleSpeed = 10f

    var combined = false // can only combine once per turn

    val sliding: Boolean
    get() = (drawX != tx * (tileWidth + tilePad * 2f) + tilePad
            || drawY != ty * (tileHeight + tilePad * 2f) + tilePad)

    init {
        val labelStyle = Label.LabelStyle(LittleGame.fonts.tileFont, white)
        label = Label("${type.ordinal + 1}", labelStyle)
        label.setAlignment(Align.center)
    }

    fun upgrade(): Int {
        // tile type increases by 1
        type = type.next()
        // update tile label
        label.setText("${type.ordinal + 1}")
        // scale tile up so that it "pulses"
        scale = 1.8f
        return if (type == TileType.STAR) 10 else 0
    }

    fun update(deltaTime: Float) {
        // slide to current position on board, if not there
        val targetDrawX = tx * (tileWidth + tilePad * 2f) + tilePad
        if (drawX != targetDrawX) {
            drawX = approach(drawX, targetDrawX, slideSpeed * deltaTime)
        }
        val targetDrawY = ty * (tileHeight + tilePad * 2f) + tilePad
        if (drawY != targetDrawY) {
            drawY = approach(drawY, targetDrawY, slideSpeed * deltaTime)
        }

        // update label position
        label.setPosition(
            drawX + tileWidth / 2f - label.width / 2f,
            drawY + tileHeight / 2f - label.height / 2f
        )

        // update scaling
        if (scale != 1f) {
            scale = approach(scale, 1f, scaleSpeed * deltaTime)
        }
    }

    fun draw() {
        LittleGame.batch.draw(
            tileImg, drawX, drawY, tileWidth / 2f, tileHeight / 2f,
            tileWidth, tileHeight, scale, scale, 0f
        )
        if (type == TileType.STAR) {
            val symbolX = drawX + tileWidth / 2f - symbolWidth / 2f
            val symbolY = drawY + tileHeight / 2f - symbolHeight / 2f
            LittleGame.batch.draw(
                starImg, symbolX, symbolY, symbolWidth / 2f, symbolHeight / 2f,
                symbolWidth, symbolHeight, scale, scale, 0f
            )
        } else {
            label.draw(LittleGame.batch, 1f)
        }
    }
}