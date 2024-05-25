package me.odinmain.features.impl.subaddons.nofeature

import me.odinmain.OdinMain.mc
import me.odinmain.features.impl.subaddons.TargetHud.healthString
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.health
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.isPlayer
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.maxHealth
import me.odinmain.utils.Vec3f
import me.odinmain.utils.corners
import me.odinmain.utils.offset
import me.odinmain.utils.render.*
import me.odinmain.utils.render.RenderUtils.blendFactor
import me.odinmain.utils.render.RenderUtils.getMatrix
import me.odinmain.utils.render.RenderUtils.renderVec
import me.odinmain.utils.render.RenderUtils.viewerVec
import me.odinmain.utils.render.RenderUtils.worldToScreen
import me.odinmain.utils.skyblock.modMessage
import me.odinmain.utils.unaryMinus
import net.minecraft.client.gui.Gui
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11
import kotlin.math.max
import kotlin.math.min

object SubRenderUtils {

    fun drawEntityFace(entity: Entity, x: Int, y: Int, ) {
        val playerInfo: NetworkPlayerInfo = mc.netHandler.getPlayerInfo(entity.uniqueID)?: return

        mc.textureManager.bindTexture(playerInfo.locationSkin)
        GL11.glColor4f(1F, 1F, 1F, 1F);

        Gui.drawScaledCustomSizeModalRect(x-5, y-5, 8f, 8f, 8,8,20,20,64f, 64f )
    }

    fun drawTargetHudInWorld(targetEntity: Entity, outline: Boolean, outlinecolor: Color, color: Color, depth: Boolean = true) {
        val mvMatrix = getMatrix(2982)
        val projectionMatrix = getMatrix(2983)
        val bb = targetEntity.entityBoundingBox.offset(-targetEntity.positionVector).offset(targetEntity.renderVec).offset(-viewerVec)
        val entity = targetEntity.renderVec.add(-targetEntity.positionVector).add(targetEntity.renderVec).add(-viewerVec).add(Vec3(0.0, 1.5, 0.0))
        var box = BoxWithClass(Float.MAX_VALUE, Float.MAX_VALUE, -1f,  -1f)

        GL11.glPushAttrib(GL11.GL_S)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GlStateManager.pushMatrix()
        GL11.glLoadIdentity()
        GL11.glOrtho(0.0, mc.displayWidth.toDouble(), mc.displayHeight.toDouble(), .0, -1.0, 1.0)
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GlStateManager.pushMatrix()
        GL11.glLoadIdentity()
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        blendFactor()
        GlStateManager.enableTexture2D()
        GlStateManager.depthMask(true)
        GL11.glLineWidth(1f)
        //start OVER

        for (boxVertex in bb.corners) {
            val screenPos = worldToScreen(
                Vec3f(boxVertex.xCoord.toFloat(), boxVertex.yCoord.toFloat(), boxVertex.zCoord.toFloat()),
                mvMatrix, projectionMatrix, mc.displayWidth, mc.displayHeight
            ) ?: continue
            box = BoxWithClass(min(screenPos.x, box.x), min(screenPos.y, box.y), max(screenPos.x, box.w), max(screenPos.y, box.h))
        }

        val screenPos = worldToScreen(
            Vec3f(entity.xCoord.toFloat(), entity.yCoord.toFloat(), entity.zCoord.toFloat()),
            mvMatrix, projectionMatrix, mc.displayWidth, mc.displayHeight)

        if (!targetEntity.isInvisible && (targetEntity !is EntityPlayer || targetEntity.isPlayer()) && (mc.thePlayer.canEntityBeSeen(targetEntity) || !depth)) {
            val distance = mc.thePlayer.getDistanceToEntity(targetEntity)


            val startX = box.x + box.w - box.x + 10f
            val startY = box.y - 25f
            fun width(width: Float) : Float { return if (distance <= 5) ( width + width * -(distance-5)) else width - distance/width }
            fun height(height: Float) : Float { return if (distance <= 5) ( height + height * -(distance-5)) else height - distance/height }
            fun x(x: Float) : Float { return x - distance/x}
            fun y(y: Float) : Float { return y - distance/y}
            targetEntity.getDistanceToEntity(mc.thePlayer)
            val x = screenPos?.x?.let { x(it + 20) } ?: startX.also { modMessage("no screenpos") }
            val y = screenPos?.y?.let { y(it) } ?: startY.also { modMessage("no screenpos") }

            modMessage(width(104f))
            if (outline) roundedRectangle(Box(x, y, width(104f), height(42f)), outlinecolor, 9f)
            //roundedRectangle(Box(startX+2, startY+2, ((42f+height/4)*2.5)-4, 38f+height/5), color, 9f)


            mc.fontRendererObj.drawString(healthString(targetEntity), startX+2+25f, startY + 2 + 15f, 1, true)
            /**drawEntityFace(targetEntity, startX.toInt() + 2 + 8, startY.toInt() + 2 + 13,)
            mcText(targetEntity.displayName?.unformattedText ?: "???", startX + 2 + 25f, startY + 2 + 5f, 1, Color.BLUE, center = false)
            mcText(healthString(targetEntity), startX + 2 + 25f, startY + 2 + 15f, 1, Color.WHITE, center = false)
            mcText(statusString(targetEntity), startX + 2 + 25f, startY + 2 + 25f, 1, Color.WHITE, center = false)*/
        }

        /**if ((box.x > 0f && box.y > 0f && box.x <= mc.displayWidth && box.y <= mc.displayHeight) || (box.w > 0 && box.h > 0 && box.w <= mc.displayWidth && box.h <= mc.displayHeight)){
            if (!targetEntity.isInvisible && (targetEntity !is EntityPlayer || targetEntity.isPlayer()) && (mc.thePlayer.canEntityBeSeen(targetEntity) || !depth)) {

                val startX = box.x + box.w - box.x + 10f
                val startY = box.y - 25f
                val width = box.w - box.x
                val height = box.h - box.y

                if (outline) roundedRectangle(Box(startX, startY, (42+height/4)*2.5, 42+height/5), outlinecolor, 9f)
                roundedRectangle(Box(startX+2, startY+2, ((42f+height/4)*2.5)-4, 38f+height/5), color, 9f)


                mc.fontRendererObj.drawString(healthString(targetEntity), startX+2+25f, startY + 2 + 15f, 1, true)
                /**drawEntityFace(targetEntity, startX.toInt() + 2 + 8, startY.toInt() + 2 + 13,)
                mcText(targetEntity.displayName?.unformattedText ?: "???", startX + 2 + 25f, startY + 2 + 5f, 1, Color.BLUE, center = false)
                mcText(healthString(targetEntity), startX + 2 + 25f, startY + 2 + 15f, 1, Color.WHITE, center = false)
                mcText(statusString(targetEntity), startX + 2 + 25f, startY + 2 + 25f, 1, Color.WHITE, center = false)*/
            }
        } */

        //end START
        GlStateManager.disableBlend()
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GlStateManager.popMatrix()
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GlStateManager.popMatrix()
        GL11.glPopAttrib()
    }

    fun drawHealthBar(targetEntity: Entity, outlinecolor: Color) {
        val mvMatrix = getMatrix(2982)
        val projectionMatrix = getMatrix(2983)
        val bb = targetEntity.entityBoundingBox.offset(-targetEntity.positionVector).offset(targetEntity.renderVec).offset(-viewerVec)
        var box = BoxWithClass(Float.MAX_VALUE, Float.MAX_VALUE, -1f,  -1f)

        GL11.glPushAttrib(GL11.GL_S)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GlStateManager.pushMatrix()
        GL11.glLoadIdentity()
        GL11.glOrtho(0.0, mc.displayWidth.toDouble(), mc.displayHeight.toDouble(), .0, -1.0, 1.0)
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GlStateManager.pushMatrix()
        GL11.glLoadIdentity()
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        blendFactor()
        GlStateManager.enableTexture2D()
        GlStateManager.depthMask(true)
        GL11.glLineWidth(1f)
        //start OVER

        for (boxVertex in bb.corners) {
            val screenPos = worldToScreen(
                Vec3f(boxVertex.xCoord.toFloat(), boxVertex.yCoord.toFloat(), boxVertex.zCoord.toFloat()),
                mvMatrix, projectionMatrix, mc.displayWidth, mc.displayHeight
            ) ?: continue
            box = BoxWithClass(min(screenPos.x, box.x), min(screenPos.y, box.y), max(screenPos.x, box.w), max(screenPos.y, box.h))
        }
        fun heightCalc(multiplier: Int = 1) : Float {
            return (box.h - box.y)/multiplier
        }
        fun getHealthPercent(entity: Entity) : Float {
            return -((entity.health()/entity.maxHealth())-1)
        }

        if ((box.x > 0f && box.y > 0f && box.x <= mc.displayWidth && box.y <= mc.displayHeight) || (box.w > 0 && box.h > 0 && box.w <= mc.displayWidth && box.h <= mc.displayHeight)) {
            roundedRectangle(box.x - heightCalc(5), box.y, heightCalc(8), heightCalc(), outlinecolor, 0f)
            roundedRectangle(box.x - heightCalc(5) + heightCalc(50), box.y + heightCalc(50), heightCalc(8)-heightCalc(25), heightCalc()-heightCalc(25), Color.GREEN, 0f)
            roundedRectangle(box.x - heightCalc(5) + heightCalc(50), box.y + heightCalc(50), heightCalc(8)-heightCalc(25), (heightCalc()-heightCalc(25)) * getHealthPercent(targetEntity), Color.RED, 0f)
        }

        //end START
        GlStateManager.disableBlend()
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GlStateManager.popMatrix()
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GlStateManager.popMatrix()
        GL11.glPopAttrib()
    }

}