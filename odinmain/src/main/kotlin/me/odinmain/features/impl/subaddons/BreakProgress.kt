package me.odinmain.features.impl.subaddons

import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.curBlockDamageMP
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.getBlockHardness
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.rnd
import me.odinmain.features.settings.impl.DualSetting
import me.odinmain.utils.render.Renderer
import me.odinmain.utils.round
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent

object BreakProgress: Module(
    name = "Break Progress",
    category = Category.SUBADDONS,
    description = "Renders the progress for blocks being broken"
) {
    val mode: Boolean by DualSetting("Mode", left = "Percent", right = "Seconds", default = false, description = "Which style to display the percent as")

    private fun setProgress() {
        if (!mode) {
            progressString = "${(100.0 * (progress / 1.0)).round(1)}%"
        } else if (mode) {
            val timeLeft: Double = (((1.0f - progress) / getBlockHardness(mc.theWorld.getBlockState(block).block, mc.thePlayer.heldItem, false, false)) / 20.0)
            val timeString: String = String.format("%.2f", timeLeft)
            progressString = if (timeLeft == 0.toDouble()) "0" else "${timeString}s"
        }
    }

    var block: BlockPos? = null
    var progress: Float = 0f
    var progressString = ""

    init {
        onClientTick { onaClientTick() }
    }

    private fun onaClientTick() {
        if (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            resetVariable()
            return
        }
        try {
            progress = curBlockDamageMP().getFloat(mc.playerController)
            if (progress == 0.0f) {
                resetVariable()
                return
            }
            block = mc.objectMouseOver.blockPos
            setProgress()
        } catch (_: IllegalAccessException) {
        }

    }

    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent) {
        if (progress != 0.0f && block != null && mc.thePlayer != null && mc.theWorld != null) {
            val pos = Vec3(block!!.x + 0.5, block!!.y + 0.5, block!!.z + 0.5)
            Renderer.drawStringInWorld(progressString, pos, depth = false)
        }
    }

    private fun resetVariable() {
        progress = 0.0f
        block = null
        progressString = ""
    }

}