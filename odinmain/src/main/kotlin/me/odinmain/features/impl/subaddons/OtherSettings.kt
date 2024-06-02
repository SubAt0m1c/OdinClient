package me.odinmain.features.impl.subaddons

import me.odinmain.OdinMain.isLegitVersion
import me.odinmain.events.impl.ChatPacketEvent
import me.odinmain.events.impl.ClickEvent
import me.odinmain.events.impl.PacketSentEvent
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.impl.dungeon.DungeonWaypoints.allowEdits
import me.odinmain.features.impl.dungeon.DungeonWaypoints.toVec3
import me.odinmain.features.impl.floor7.p3.TerminalSolver
import me.odinmain.features.impl.floor7.p3.TerminalTypes
import me.odinmain.features.settings.Setting.Companion.withDependency
import me.odinmain.features.settings.impl.BooleanSetting
import me.odinmain.features.settings.impl.ColorSetting
import me.odinmain.features.settings.impl.SelectorSetting
import me.odinmain.utils.equal
import me.odinmain.utils.equalsOneOf
import me.odinmain.utils.render.Color
import me.odinmain.utils.skyblock.*
import me.odinmain.utils.skyblock.EtherWarpHelper.etherPos
import me.odinmain.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.util.ChatComponentText
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object OtherSettings : Module(
    name = "Other Settings",
    category = Category.SUBADDONS,
    description = "Settings for other modules"
) {
    private val legitSettings by BooleanSetting("Legit Settings", false, description = "Allows some cheater features on legit").withDependency { isLegitVersion }
    val terminals: Boolean by BooleanSetting("Block Wrong Terms", false, description = "Blocks wrong clicks in terminals.").withDependency { legitSettings }
    val gyro: Boolean by BooleanSetting("Block align", false, description = "Blocks Align")
    val tempwaypointsAnywhere: Boolean by BooleanSetting("Temp Waypoints anywhere", true, description = "Allows temp waypoints anywhere, specifically outside skyblock")
    private val noEther: Boolean by BooleanSetting("No Ether", default = false, description = "Stops you from etherwarping to certain waypoints/blocks. REQUIRES etherwarp helper")
    private var color: Color by ColorSetting("Block Color", default = Color.GREEN, description = "The color of waypoints that block etherwarps.", allowAlpha = true).withDependency { colorPallet == 0  && noEther}
    private val colorPallet: Int by SelectorSetting("Color pallet", "Red", arrayListOf("None", "Aqua", "Magenta", "Yellow", "Lime", "Red")).withDependency { noEther }
    private val fmeCompat: Boolean by BooleanSetting("FME compatability", default = false, description = "Allows for fme compatability. Uses redstone blocks.").withDependency { noEther }
    //private val sbeBloodFix: Boolean by BooleanSetting("SBE Blood Fix", default = false, description = "Fixes sbe's blood camp helper")
    private val editqol: Boolean by BooleanSetting("Edit Mode QOL", false, description = "auto disabled edit mode on world load and stops it from being enabled outside of dungeons")
    val antiBot: Boolean by BooleanSetting("AntiBot", default = true, description = "Disables highlights and such when a player is a bot. This method is very rudimentary and could be easily patched.")
    val telemetry: Boolean by BooleanSetting("Send Telemetry Data", default = true, description = "Sends SubAt0mic join and version logs. Turn off to disable.")

    fun color(): Color {
        val color: Color = when (colorPallet) {
            0 -> color
            1 -> Color.CYAN
            2 -> Color.MAGENTA
            3 -> Color.YELLOW
            4 -> Color.GREEN
            5 -> Color.RED
            else -> color
        }
        return color
    }

    @SubscribeEvent
    fun onRightClick(event: ClickEvent.RightClickEvent) {
        if (heldItem?.itemID == "GYROKINETIC_WAND" && gyro) { event.isCanceled = true }

        if (
            noEther &&
            (DungeonUtils.currentRoom?.waypoints?.any { etherPos.vec?.equal(it.toVec3()) == true && (it.color == color()) } == true ||
            (getBlockIdAt(etherPos.pos ?: return) == 152 && fmeCompat)) &&
            mc.thePlayer.isSneaking &&
            mc.thePlayer.holdingEtherWarp
        ) { event.isCanceled = true }
    }

    @SubscribeEvent
    fun onSlotClick(event: PacketSentEvent) {
        if (!terminals) return
        if (event.packet !is C0EPacketClickWindow || TerminalSolver.currentTerm == TerminalTypes.NONE) return
        if ((event.packet as C0EPacketClickWindow).slotId in TerminalSolver.solution) return
        event.isCanceled = true
    }

    /**
    val terminalMap = mutableMapOf<String, terminalPlayer>() // "player": {terminal: 0, device: 0, lever: 0}

    data class terminalPlayer(var terminals: Int, var devices: Int, var devers: Int) {
        fun increment(type: String) {
            when (type) {
                "terminal" -> terminals++
                "device" -> devices++
                "lever" -> devers++
                else -> throw IllegalArgumentException("Invalid type: $type")
            }
        }
    }
    val terminalRegex = Regex("^(\\w{1,16}) (?:activated|completed) a (\\w+)! \\(\\d/\\d\\)$")
    */
    init {
        onWorldLoad {
            if (editqol) allowEdits = false
            //terminalMap.clear()
        }
        /**onMessage(Regex("\\[BOSS] The Watcher:(.*)")) {
            if (!sbeBloodFix) return@onMessage
            if (it.equalsOneOf(
                    "[BOSS] The Watcher: Congratulations, you made it through the Entrance.",
                    "[BOSS] The Watcher: Ah, you've finally arrived.",
                    "[BOSS] The Watcher: Ah, we meet again...",
                    "[BOSS] The Watcher: So you made it this far... interesting.",
                    "[BOSS] The Watcher: You've managed to scratch and claw your way here, eh?",
                    "[BOSS] The Watcher: I'm starting to get tired of seeing you around here...",
                    "[BOSS] The Watcher: Oh.. hello?",
                    "[BOSS] The Watcher: Things feel a little more roomy now, eh?"
            )) {
                mc.thePlayer.addChatMessage(ChatComponentText("§r§cThe §r§c§lBLOOD DOOR§r§c has been opened!"))
                MinecraftForge.EVENT_BUS.post(ChatPacketEvent("§r§cThe §r§c§lBLOOD DOOR§r§c has been opened!"))
            }
        } */
        /**
        onMessage(Regex("\\[BOSS] Goldor: Who dares trespass into my domain?")) {
            dungeonTeammates.forEach{ player ->
                terminalMap[player.name] = terminalPlayer(0,0,0)
            }
        }

        onMessage(terminalRegex) {
            val player = terminalRegex.matchEntire(it)?.groups?.get(1)?.value ?: return@onMessage
            val type = terminalRegex.matchEntire(it)?.groups?.get(2)?.value ?: return@onMessage
            val data = terminalMap[player]
            data?.increment(type)
        }

        onMessage(Regex("The Core entrance is opening!")) {
            val messageBuild = StringBuilder()
            terminalMap.forEach { (player, data) ->
                val formatted = dungeonTeammates.find { it.name == player }
                messageBuild.append("\n§d$formatted §8| §6${data.terminals} §aTerminals §8| §6${data.devices} §aDevices §8| §6${data.devers} §aLevers")
            }
            if (messageBuild.isNotEmpty()) {
                modMessage("§8Terminals: ${messageBuild.toString().trim()}")
            }
        } */
    }

}