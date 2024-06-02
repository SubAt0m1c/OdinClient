package me.odinmain.features.impl.subaddons.nofeature

import me.odinmain.OdinMain.mc
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.health
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.healthColor
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.healthPercent
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.maxHealth
import me.odinmain.utils.Vec3f
import me.odinmain.utils.corners
import me.odinmain.utils.offset
import me.odinmain.utils.render.*
import me.odinmain.utils.render.RenderUtils.blendFactor
import me.odinmain.utils.render.RenderUtils.getMatrix
import me.odinmain.utils.render.RenderUtils.getRenderPos
import me.odinmain.utils.render.RenderUtils.renderVec
import me.odinmain.utils.render.RenderUtils.viewerVec
import me.odinmain.utils.render.RenderUtils.worldToScreen
import me.odinmain.utils.unaryMinus
import net.minecraft.client.gui.Gui
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import org.lwjgl.opengl.GL11
import kotlin.math.max
import kotlin.math.min


object SubRenderUtils {

    fun drawEntityFace(entity: Entity, x: Int, y: Int, scale: Float = 1f) {
        val playerInfo: NetworkPlayerInfo = mc.netHandler.getPlayerInfo(entity.uniqueID)?: return

        mc.textureManager.bindTexture(playerInfo.locationSkin)
        GL11.glColor4f(1F, 1F, 1F, 1F);

        Gui.drawScaledCustomSizeModalRect(x-5, y-5, 8f, 8f, 8 ,8, (20 * scale).toInt(), (20 * scale).toInt(),64f, 64f)
    }

    /**fun drawTargetHudInWorld(targetEntity: Entity, outline: Boolean, outlinecolor: Color, color: Color, depth: Boolean = true) {
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
    } */

    fun drawHealthBar(targetEntity: Entity) {
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
        
        fun hc(multiplier: Int = 1) : Float { return (box.h - box.y) / multiplier }

        if ((box.x > 0f && box.y > 0f && box.x <= mc.displayWidth && box.y <= mc.displayHeight) || (box.w > 0 && box.h > 0 && box.w <= mc.displayWidth && box.h <= mc.displayHeight)) {
            roundedRectangle(box.x - hc(5), box.y - hc(8), hc(13), hc() + hc(7), Color.BLACK, 0f)
            roundedRectangle(box.x - hc(5) + hc(74), box.y + hc(74) - hc(8), hc(13)-hc(37), hc()-hc(37)+hc(7), targetEntity.healthColor(), 0f)
            roundedRectangle(box.x - hc(5) + hc(74), box.y + hc(74) - hc(8), hc(13)-hc(37), (hc()-hc(37)+hc(7)) * remainingHealth(targetEntity), Color.DARK_GRAY, 0f)
            //roundedRectangle(box.x - hc(5) + hc(74), box.y + hc(74) - hc(8), hc(13)-hc(37), (hc()-hc(37)+hc(7) * absorbtionHealth(targetEntity)), Color.BLUE, 0f)
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

    fun drawHealthBarInWorld(e: Entity, expand: Double, shift: Double) {
        if (e is EntityLivingBase) {
            val renderPos = getRenderPos(e.renderVec)
            val expand2 = expand.toFloat() / 40.0f
            val hp = e.healthPercent().toDouble()
            val op = if (hp > 1) ((hp - 1)%(1)) else 0.0
            val nohp = (min(1.0, hp))
            fun ht(b: Double) : Int { return (74.0 * b).toInt() }
            val hc: Int = e.healthColor().rgba

            //rendering begin
            GlStateManager.pushMatrix()
            GL11.glTranslated(renderPos.xCoord, renderPos.yCoord - 0.2, renderPos.zCoord)
            GL11.glRotated((-mc.renderManager.playerViewY).toDouble(), 0.0, 1.0, 0.0)
            GlStateManager.disableDepth()
            GL11.glScalef(0.03f + expand2, 0.03f + expand2, 0.03f + expand2)
            val i = (21.0 + shift * 2.0).toInt()
            Gui.drawRect(i, -1, i + 5, 75, Color.BLACK.rgba)
            Gui.drawRect(i + 1, ht(nohp), i + 4, 74, Color.DARK_GRAY.rgba)
            Gui.drawRect(i + 1, 0, i + 4, ht(nohp), hc)
            Gui.drawRect(i + 1, 74, i + 4, 74-ht(op), Color.BLUE.rgba)
            GlStateManager.enableDepth()
            GlStateManager.popMatrix()
        }
    }

    private fun remainingHealth(entity: Entity) : Float { return -(entity.healthPercent()-1) }

    private fun absorbtionHealth(entity: Entity) : Float {
        val hp = entity.healthPercent().toDouble()
        val op = if (hp > 1) ((hp - 1)%(1)) else 0.0
        return op.toFloat()
    }

    private fun color(entity: Entity) : Color {
        val hp = remainingHealth(entity)
        return when {
            hp <= 0 -> Color.DARK_GREEN
            hp <= 0.25 -> Color.GREEN
            hp <= 0.75 -> Color.ORANGE
            hp > 0.75 -> Color.RED
            else -> Color.DARK_GREEN
        }
    }

}