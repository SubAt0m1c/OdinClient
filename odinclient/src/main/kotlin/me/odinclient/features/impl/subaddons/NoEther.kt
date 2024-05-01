package me.odin.features.impl.subaddons

import me.odinclient.mixin.accessors.IEntityPlayerSPAccessor
import me.odinmain.events.impl.ClickEvent
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.settings.impl.BooleanSetting
import me.odinmain.features.settings.impl.DualSetting
import me.odinmain.features.settings.impl.StringSetting
import me.odinmain.utils.PositionLook
import me.odinmain.utils.render.RenderUtils.renderVec
import me.odinmain.utils.skyblock.*
import me.odinmain.utils.skyblock.EtherWarpHelper
import me.odinmain.utils.skyblock.EtherWarpHelper.etherPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object NoEther : Module (
    name = "No Ether",
    category = Category.SUBADDONS,
    description = "Cancels etherwarps when looking at Redstone Blocks. Only works with FME"
) {
    private val useServerPosition: Boolean by DualSetting("Positioning", "Server Pos", "Player Pos", description = "If etherwarp guess should use your server position or real position.")

    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent) {
        val player = mc.thePlayer as? IEntityPlayerSPAccessor ?: return
        val positionLook =
            if (useServerPosition)
                PositionLook(Vec3(player.lastReportedPosX, player.lastReportedPosY, player.lastReportedPosZ), player.lastReportedYaw, player.lastReportedPitch)
            else
                PositionLook(mc.thePlayer.renderVec, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)

        etherPos = EtherWarpHelper.getEtherPos(positionLook)
    }

    @SubscribeEvent
    fun onClick(event: ClickEvent.RightClickEvent) {
        if (mc.thePlayer.isSneaking && mc.thePlayer.heldItem.extraAttributes?.getBoolean("ethermerge") == true && (etherPos.succeeded)) {
            val pos = etherPos.pos ?: return
            val block = getBlockIdAt(pos)
            if (block == 152) event.isCanceled = true
        }
    }

}