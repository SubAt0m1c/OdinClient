package me.odinmain.features.impl.subaddons.nofeature

import me.odinmain.OdinMain.mc
import me.odinmain.features.impl.subaddons.TargetHud
import me.odinmain.utils.ServerUtils.getPing
import me.odinmain.utils.ServerUtils.onWorldLoad
import me.odinmain.utils.cleanSB
import me.odinmain.utils.clock.Executor
import me.odinmain.utils.clock.Executor.Companion.register
import me.odinmain.utils.skyblock.LocationUtils.onHypixel
import net.minecraft.client.gui.Gui
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLiving
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11

object SubUtils {

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        EntityHealthData.clear()
    }

    private val EntityHealthData = mutableMapOf<String, Pair<Float, Float>>()

    /**
     * Gets the health of the specified entity.
     *
     * @return health as a float.
     */
    fun Entity.health(): Float {
        healthData(this)
        return EntityHealthData[this.name]?.first ?: 0f
    }

    /**
     * Gets the max health of the specified entity.
     *
     * @return max health as a float.
     */
    fun Entity.maxHealth(): Float {
        healthData(this)
        return EntityHealthData[this.name]?.second ?: 0f
    }

    /**
     * handles getting the health of an entity
     *
     * This function checks if the entity is a player, and if so, gets their scoreboard data. Otherwise, its gets the entities health.
     * After it gets the health, it posts it to a health map.
     */
    private fun healthData(entity: Entity) {
        var health: Float = 0f
        var maxHealth: Float = 0f
        if (entity is EntityPlayer) {
            maxHealth = 20f
            mc.thePlayer.worldScoreboard.scoreObjectives.forEach { score ->
                val healthData = score.scoreboard.scores.find { it.playerName == entity.name }?.scorePoints?.toFloat() ?: 0f.also { maxHealth = 0f }
                if (EntityHealthData[entity.name]?.first == healthData) return else health = healthData
            }
        } else if (entity is EntityLiving) {
            val healthData = entity.health
            if (EntityHealthData[entity.name]?.first == healthData) return else health = healthData
            maxHealth = entity.maxHealth
        }
        EntityHealthData[entity.name] = Pair(health, maxHealth)
    }

    /**
     * Determines whether a player is on the same team as the user.
     *
     * This function checks if the users prefix color (§*) are the same as the players.
     *
     * @return `true` if the specified player is on the same team as the user, otherwise `false`.
     */
    fun Entity.isOnTeam(): Boolean {
        if (mc.thePlayer.displayName.unformattedText.startsWith("§")) {
            if (mc.thePlayer.displayName.unformattedText.length <= 2 || this.displayName.unformattedText.length <= 2) return false
            if (mc.thePlayer.displayName.unformattedText.substring(0, 2) == this.displayName.unformattedText.substring(0, 2)) return true
        }
        return false
    }

    /**
     * Determines whether a player is It in TNT tag.
     *
     * This function checks if the players prefix contains It"
     *
     * @return `true` if the specified player is it, otherwise `false`.
     */
    fun Entity.isIt(): Boolean {
        return this.displayName.unformattedText.startsWith("§c[IT]")
    }


    /**
     * Determines whether an Entity is a player.
     *
     * This function checks if the player's ping equals 1
     *
     * @return `true` if the specified player's ping equals 1, otherwise `false`.
     */
    fun Entity.isPlayer(): Boolean {
        return this.getPing() == 1 //this method doesn't work in lobbies, for some reason.
    }


    /**
     * Sends a message using the SubAddons prefix.
     *
     * This function sends a message beginning with "§eSubAddons §8»§r"
     */
    fun subMessage(message: Any?, prefix: Boolean = true, chatStyle: ChatStyle? = null) {
        if (mc.thePlayer == null) return
        val chatComponent = ChatComponentText(if (prefix) "§eSubAddons §8»§r $message" else message.toString())
        chatStyle?.let { chatComponent.setChatStyle(it) } // Set chat style using setChatStyle method
        try { mc.thePlayer?.addChatMessage(chatComponent) }
        catch (e: Exception) { e.printStackTrace() }
    }

    var inTNTTag: Boolean = false
    var read = false


    init {
        Executor(500) {
            if (!inTNTTag) {
                inTNTTag = onHypixel && mc.theWorld.scoreboard.getObjectiveInDisplaySlot(1)
                    ?.let { cleanSB(it.displayName).contains("TNT TAG") } ?: false
            }
        }.register()
    }

    fun drawEntityFace(entity: Entity, x: Int, y: Int, ) {
        val playerInfo: NetworkPlayerInfo = mc.netHandler.getPlayerInfo(entity.uniqueID)?: return

        mc.textureManager.bindTexture(playerInfo.locationSkin)
        GL11.glColor4f(1F, 1F, 1F, 1F);

        Gui.drawScaledCustomSizeModalRect(x-5, y-5, 8f, 8f, 8,8,20,20,64f, 64f )
    }

}