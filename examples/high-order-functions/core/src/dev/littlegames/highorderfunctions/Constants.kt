package dev.littlegames.highorderfunctions

import com.badlogic.gdx.graphics.Color

object Constants {
    const val tileWidth = 140f
    const val tileHeight = 140f
    const val symbolWidth = 100f
    const val symbolHeight = 100f
    const val tilePad = 5f
    const val rows = 4
    const val cols = 4
    const val uiHeight = tileHeight
    const val viewWidth = (tileWidth + tilePad * 2) * cols
    const val viewHeight = (tileHeight + tilePad * 2) * rows + uiHeight
    val black = Color(0f/255, 43f/255, 54f/255, 1f)
    val darkBlue = Color(19f/255, 91f/255, 132f/255, 1f)
    val lightBlue = Color(38f/255, 139f/255, 210f/255, 1f)
    val white = Color(245f/255, 248f/255, 253f/255, 1f)
}