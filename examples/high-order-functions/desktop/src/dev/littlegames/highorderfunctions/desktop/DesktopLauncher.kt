package dev.littlegames.highorderfunctions.desktop

import kotlin.jvm.JvmStatic
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import dev.littlegames.highorderfunctions.Constants.viewHeight
import dev.littlegames.highorderfunctions.Constants.viewWidth
import dev.littlegames.highorderfunctions.LittleGame

object DesktopLauncher {
    @JvmStatic
    fun main(arg: Array<String>) {
        val config = LwjglApplicationConfiguration()
        config.width = viewWidth.toInt()
        config.height = viewHeight.toInt()
        config.fullscreen = false
        LwjglApplication(LittleGame(), config)
    }
}