package me.odinmain.features.impl.subaddons

import me.odinmain.OdinMain.isLegitVersion
import me.odinmain.events.impl.ClickEvent
import me.odinmain.events.impl.PacketSentEvent
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.impl.dungeon.DungeonWaypoints.toVec3
import me.odinmain.features.impl.floor7.p3.TerminalSolver
import me.odinmain.features.impl.floor7.p3.TerminalTypes
import me.odinmain.features.settings.Setting.Companion.withDependency
import me.odinmain.features.settings.impl.BooleanSetting
import me.odinmain.features.settings.impl.ColorSetting
import me.odinmain.features.settings.impl.SelectorSetting
import me.odinmain.utils.equal
import me.odinmain.utils.render.Color
import me.odinmain.utils.skyblock.*
import me.odinmain.utils.skyblock.EtherWarpHelper.etherPos
import me.odinmain.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object OtherSettings : Module(
    name = "Other Settings",
    category = Category.SUBADDONS,
    description = "Settings for other modules"
) {
    private val legitSettings by BooleanSetting("Legit Settings", false, description = "Allows some cheater features on legit").withDependency { isLegitVersion }
    val gyro: Boolean by BooleanSetting("Block align", false, description = "Blocks Align").withDependency { legitSettings }
    val terminals: Boolean by BooleanSetting("Block Wrong Terms", false, description = "Blocks wrong clicks in terminals.").withDependency { legitSettings }
    val tempwaypointsAnywhere: Boolean by BooleanSetting("Temp Waypoints anywhere", true, description = "Allows temp waypoints anywhere, specifically outside skyblock")
    private val noEther: Boolean by BooleanSetting("No Ether", default = false, description = "Stops you from etherwarping to certain waypoints/blocks. REQUIRES etherwarp helper")
    private var color: Color by ColorSetting("Block Color", default = Color.GREEN, description = "The color of waypoints that block etherwarps.", allowAlpha = true).withDependency { colorPallet == 0  && noEther}
    private val colorPallet: Int by SelectorSetting("Color pallet", "Red", arrayListOf("None", "Aqua", "Magenta", "Yellow", "Lime", "Red")).withDependency { noEther }
    private val fmeCompat: Boolean by BooleanSetting("FME compatability", default = false, description = "Allows for fme compatability. Uses redstone blocks.").withDependency { noEther }

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
        if (heldItem?.itemID == "GYROKINETIC_WAND" && gyro) {
            event.isCanceled = true
        }

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
}