package dev.littlegames.hybrid.desktop

import kotlin.jvm.JvmStatic
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import dev.littlegames.hybrid.Constants.viewHeight
import dev.littlegames.hybrid.Constants.viewScale
import dev.littlegames.hybrid.Constants.viewWidth
import dev.littlegames.hybrid.LittleGame

object DesktopLauncher {
    @JvmStatic
    fun main(arg: Array<String>) {
        val config = LwjglApplicationConfiguration()
        config.resizable = false
        config.width = viewWidth.toInt() * viewScale
        config.height = viewHeight.toInt() * viewScale
        config.fullscreen = false
        LwjglApplication(LittleGame(), config)
    }
}