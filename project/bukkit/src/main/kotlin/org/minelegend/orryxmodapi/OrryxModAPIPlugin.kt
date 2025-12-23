package org.minelegend.orryxmodapi

import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info

@PlatformSide(Platform.BUKKIT)
object OrryxModAPIPlugin : Plugin() {

    override fun onEnable() {
        info("OrryxModAPI Enable")
    }
}