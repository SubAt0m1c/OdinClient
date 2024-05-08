package me.odinmain.features.impl.subaddons

import me.odinmain.OdinMain.mc
import me.odinmain.utils.ServerUtils.getPing
import me.odinmain.utils.cleanSB
import me.odinmain.utils.clock.Executor
import me.odinmain.utils.clock.Executor.Companion.register
import me.odinmain.utils.skyblock.LocationUtils.inSkyblock
import me.odinmain.utils.skyblock.LocationUtils.onHypixel
import net.minecraft.entity.Entity
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle

object SubUtils {

    fun Entity.isOnTeam(): Boolean {
        if (mc.thePlayer.displayName.unformattedText.startsWith("§")) {
            if (mc.thePlayer.displayName.unformattedText.length <= 2 || this.displayName.unformattedText.length <= 2) return false
            if (mc.thePlayer.displayName.unformattedText.substring(0, 2) == this.displayName.unformattedText.substring(0, 2)) return true
        }
        return false
    }

    fun Entity.isIt(): Boolean {
        return this.displayName.unformattedText.startsWith("§c[IT]")
    }

    fun Entity.isPlayer(): Boolean {
        return this.getPing() == 1 //this method doesn't work in lobbies, for some reason.
    }

    fun subMessage(message: Any?, prefix: Boolean = true, chatStyle: ChatStyle? = null) {
        if (mc.thePlayer == null) return
        val chatComponent = ChatComponentText(if (prefix) "§eSubAddons §8»§r $message" else message.toString())
        chatStyle?.let { chatComponent.setChatStyle(it) } // Set chat style using setChatStyle method
        try { mc.thePlayer?.addChatMessage(chatComponent) }
        catch (e: Exception) { e.printStackTrace() }
    }

    var inTNTTag: Boolean = false

    init {
        Executor(500) {
            if (!inTNTTag) {
                inTNTTag = onHypixel && mc.theWorld.scoreboard.getObjectiveInDisplaySlot(1)
                    ?.let { cleanSB(it.displayName).contains("TNT TAG") } ?: false
            }
        }.register()
    }

}