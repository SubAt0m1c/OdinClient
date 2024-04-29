package me.odinmain.features.impl.subaddons

import me.odinmain.OdinMain.isLegitVersion
import me.odinmain.events.impl.ClickEvent
import me.odinmain.events.impl.PacketSentEvent
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.impl.floor7.p3.TerminalSolver
import me.odinmain.features.impl.floor7.p3.TerminalTypes
import me.odinmain.features.settings.Setting.Companion.withDependency
import me.odinmain.features.settings.impl.BooleanSetting
import me.odinmain.utils.skyblock.heldItem
import me.odinmain.utils.skyblock.itemID
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


    @SubscribeEvent
    fun onRightClick(event: ClickEvent.RightClickEvent) {
        if (heldItem?.itemID != "GYROKINETIC_WAND" || !gyro) return
        event.isCanceled = true
    }

    @SubscribeEvent
    fun onSlotClick(event: PacketSentEvent) {
        if (!terminals) return
        if (event.packet !is C0EPacketClickWindow || TerminalSolver.currentTerm == TerminalTypes.NONE) return
        if ((event.packet as C0EPacketClickWindow).slotId in TerminalSolver.solution) return
        event.isCanceled = true
    }
}