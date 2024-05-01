package me.odin.features.impl.render

import me.odin.mixin.accessors.IEntityPlayerSPAccessor
import me.odinmain.events.impl.ClickEvent
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.settings.Setting.Companion.withDependency
import me.odinmain.features.settings.impl.BooleanSetting
import me.odinmain.features.settings.impl.ColorSetting
import me.odinmain.features.settings.impl.DualSetting
import me.odinmain.features.settings.impl.NumberSetting
import me.odinmain.ui.clickgui.util.ColorUtil.withAlpha
import me.odinmain.utils.PositionLook
import me.odinmain.utils.render.Color
import me.odinmain.utils.render.RenderUtils.renderVec
import me.odinmain.utils.render.Renderer
import me.odinmain.utils.skyblock.*
import me.odinmain.utils.skyblock.EtherWarpHelper
import me.odinmain.utils.skyblock.EtherWarpHelper.etherPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object EtherWarpHelper : Module(
    name = "Ether Warp Helper",
    description = "Shows you where your etherwarp will teleport you.",
    category = Category.RENDER
) {
    private val render: Boolean by BooleanSetting("Show Etherwarp Guess", true)
    private val useServerPosition: Boolean by DualSetting("Positioning", "Server Pos", "Player Pos", description = "If etherwarp guess should use your server position or real position.").withDependency { render }
    private val renderColor: Color by ColorSetting("Color", Color.ORANGE.withAlpha(.5f), allowAlpha = true)
    private val renderFail: Boolean by BooleanSetting("Show when failed", true)
    private val wrongColor: Color by ColorSetting("Wrong Color", Color.RED.withAlpha(.5f), allowAlpha = true).withDependency { renderFail }
    private val filled: Boolean by DualSetting("Type", "Outline", "Filled", default = false)
    private val thickness: Float by NumberSetting("Thickness", 3f, 1f, 10f, .1f).withDependency { !filled }
    private val phase: Boolean by BooleanSetting("Phase", false)

    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent) {
        val player = mc.thePlayer as? IEntityPlayerSPAccessor ?: return
        val positionLook =
            if (useServerPosition)
                PositionLook(Vec3(player.lastReportedPosX, player.lastReportedPosY, player.lastReportedPosZ), player.lastReportedYaw, player.lastReportedPitch)
            else
                PositionLook(mc.thePlayer.renderVec, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)

        etherPos = EtherWarpHelper.getEtherPos(positionLook)
        if (render && mc.thePlayer.isSneaking && mc.thePlayer.heldItem.extraAttributes?.getBoolean("ethermerge") == true && (etherPos.succeeded || renderFail)) {
            val pos = etherPos.pos ?: return
            val color = if (etherPos.succeeded) renderColor else wrongColor
            val aabb = getBlockAt(pos).getSelectedBoundingBox(mc.theWorld, pos) ?: return
            if (filled)
                Renderer.drawBox(aabb, color, depth = phase, outlineAlpha = 0)
            else
                Renderer.drawBox(aabb, color, outlineWidth = thickness, depth = phase, fillAlpha = 0)
        }
    }

}