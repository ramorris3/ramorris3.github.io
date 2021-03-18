package dev.littlegames.highorderfunctions

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import dev.littlegames.highorderfunctions.Constants.darkBlue
import dev.littlegames.highorderfunctions.Constants.lightBlue
import dev.littlegames.highorderfunctions.Constants.white

class Fonts {
    val titleFont: BitmapFont
    val tileFont: BitmapFont
    val defaultFont: BitmapFont

    init {
        val generator = FreeTypeFontGenerator(Gdx.files.internal("CutiveMono-Regular.ttf"))
        var parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = 72
        parameter.color = white
        parameter.shadowColor = darkBlue
        parameter.shadowOffsetX = 4
        parameter.shadowOffsetY = 4
        titleFont = generator.generateFont(parameter)

        parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = 100
        parameter.color = white
        parameter.borderColor = lightBlue
        parameter.borderWidth = 2f
        tileFont = generator.generateFont(parameter)

        parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = 20
        parameter.color = white
        defaultFont = generator.generateFont(parameter)
    }

    fun dispose() {
        titleFont.dispose()
        defaultFont.dispose()
    }
}