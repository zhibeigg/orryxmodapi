package org.minelegend.orryxmodapi

import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.Plugin
import taboolib.platform.BungeePlugin

@PlatformSide(Platform.BUNGEE)
object OrryxModBungeecodePlugin: Plugin() {

    override fun onEnable() {
        BungeePlugin.getInstance().proxy.registerChannel("orryxmod:main")
    }
}