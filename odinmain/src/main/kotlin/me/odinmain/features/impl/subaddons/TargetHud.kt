package me.odinmain.features.impl.subaddons

import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.impl.subaddons.nofeature.SubRenderUtils.drawHealthBarInWorld
import me.odinmain.features.impl.subaddons.nofeature.SubRenderUtils.drawEntityFace
import me.odinmain.features.impl.subaddons.nofeature.SubRenderUtils.drawHealthBar
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.colorHealth
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.health
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.healthColor
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.healthPercent
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.isPlayer
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.maxHealth
import me.odinmain.features.settings.Setting.Companion.withDependency
import me.odinmain.features.settings.impl.*
import me.odinmain.ui.hud.HudElement
import me.odinmain.utils.*
import me.odinmain.utils.clock.Clock
import me.odinmain.utils.render.*
import me.odinmain.utils.skyblock.devMessage
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLiving
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemAppleGold
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.entity.player.AttackEntityEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object TargetHud : Module(
    name = "Target Hud",
    description = "Renders info of the target entity",
    category = Category.SUBADDONS
){
    private val color: Color by ColorSetting("Color", Color.DARK_GRAY, allowAlpha = true)
    private val outline: Boolean by BooleanSetting("Outline", true)
    private val outlinecolor: Color by ColorSetting("Outline Color", Color.BLUE, allowAlpha = true).withDependency { outline }
    private val rounded: Boolean by BooleanSetting("Rounded Edges", default = true, description = "draws the box with rounded edges")
    private val centered: Boolean by BooleanSetting("Centered", default = true, description = "Centers the target hud")
    private val compact: Boolean by BooleanSetting("Compact", default = false, description = "Compact mode of target hud")
    private val healthBar: Boolean by BooleanSetting("Health bar", description = "displays a health bar next to the target")
    private val style: Boolean by DualSetting("Style", left = "scale", right = "Object", default = false).withDependency { healthBar }
    private val expand: Double by NumberSetting("Expand", 0.0, -0.3, 2.0, 0.1, description = "Expand the health bar").withDependency { healthBar && style}
    private val xShift: Double by NumberSetting("X Shift", 0.0, -35, 10, 1.0, description = "Offset the X of the health bar").withDependency { healthBar && style }

    private val hud: HudElement by HudSetting("Display", 10f, 10f, 2f, false) {
        val targetEntity = target
        if (it) {
            val startX = if (centered) -50f else 0f
            if (outline) roundedRectangle(startX-2f, -2f, 100 + 4f, 42f, outlinecolor)
            roundedRectangle(startX, 0f, 100f, 38f, color)
        } else if (targetEntity != null && !targetEntity.isInvisible && (targetEntity !is EntityPlayer || targetEntity.isPlayer())) {
            //global values
            var start = when {
                targetEntity is EntityPlayer && compact -> 22f
                targetEntity is EntityPlayer -> 30f
                else -> 0f
            }

            val height = if (compact) 22f else 37f
            val width = if (compact) getMCTextWidth(nameString(targetEntity)) + start + 8f else
                max(getMCTextWidth(nameString(targetEntity)), getMCTextWidth(healthString(targetEntity)), getMCTextWidth(statusString(targetEntity))) + start + 8f
            val roundness = if (rounded) 9f else 0f

            val renderStart = if (centered) 0f - width/2 else 0f
            start += renderStart

            //outline render
            if (outline )roundedRectangle(renderStart-2f, -2f, width + 4f, height + 4f, outlinecolor, roundness)

            //background render
            roundedRectangle(renderStart, 0f, width, height, color, roundness)

            //healthbar values
            val barX = if (compact) start else renderStart
            val barW = if (compact) width - start + renderStart else width

            val hp = targetEntity.healthPercent()
            val healthWidth = barW * min(hp, 1f)
            val abHealthWidth = barW * if (hp > 1) ((hp - 1)%(1)) else 0.0
            val barY = height - 5
            val barRound = if (rounded) 3f else 0f

            //healthbar render
            if (hp < 1) roundedRectangle(barX, barY, barW, 5, Color.DARK_GRAY, barRound) //background
            roundedRectangle(barX, barY, healthWidth, 5, targetEntity.healthColor(), barRound) //health
            if (hp > 1) roundedRectangle((barX+barW - abHealthWidth), barY, abHealthWidth, 5f, Color.BLUE, barRound) //absorption

            //text
            if (targetEntity is EntityPlayer) {
                if (compact) drawEntityFace(targetEntity, renderStart.toInt(), 0, height.toInt() + 2, height.toInt() + 2)
                else drawEntityFace(targetEntity, renderStart.toInt() + 2, 3, 28, 28)
                start += 3
            }

            mcText(nameString(targetEntity), start, if (compact) 5f else 3f, 1, Color.BLUE, center = false)
            if (!compact) {
                mcText(healthString(targetEntity), start, 13, 1, Color.WHITE, center = false)
                mcText(statusString(targetEntity), start, 23f, 1, Color.WHITE, center = false)
            }
        }
        100f to 38f
    }

    private val targetCD = Clock(3_000)
    //https://regex101.com/r/39ctT3/3
    private val triggerRegex = Regex("^(\\w{1,16}) (?:➡|is on) (\\d*\\.?\\d*)(?:❤ \\(-\\d*\\.?\\d*❤\\)| HP!)\$")

    var target: Entity? = null

    @SubscribeEvent
    fun onAttack(event: AttackEntityEvent){
        val entity = mc.theWorld.getEntityByID(event.target.entityId) ?: return
        if ((entity !is EntityPlayer && entity !is EntityLiving) || entity.isInvisible || !entity.isEntityAlive) return
        target = entity
        targetCD.update()
    }


    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (target?.isDead == true && !targetCD.hasTimePassed()) {
            val targetEntity = target ?: return
            runIn(15) { if (!targetCD.hasTimePassed() && target == targetEntity) target = null }
        }

        val entity = mc.pointedEntity
        if (targetCD.hasTimePassed() && target != null) target = null

        if (entity != null && (entity is EntityPlayer || entity is EntityLiving) && !entity.isInvisible && entity.isEntityAlive) target = entity

        if (
            healthBar && target != null &&
            mc.thePlayer.canEntityBeSeen(target) &&
            target?.isInvisible == false
            && (target?.isPlayer() == true || entity !is EntityPlayer)
            )  {
            if (!style) drawHealthBar(target ?: return) else drawHealthBarInWorld(target ?: return, expand, xShift)
        }
        //if (followPlayer && target?.entity != null) drawTargetHudInWorld(target?.entity ?: return, outline, outlinecolor, color)
    }

    private fun winStatus(entity: Entity): String {
        return when {
            entity.health() > mc.thePlayer.health -> "§l§cL"
            entity.health() == mc.thePlayer.health -> "§3T"
            else -> "§l§aW"
        }
    }

    private fun nameString(entity: Entity): String {
        return if (compact) "${entity.displayName.unformattedText} ${entity.colorHealth(true)} ${winStatus(entity)}" else entity.displayName.unformattedText
    }

    private fun healthString(entity: Entity): String {
        return "${entity.colorHealth()}§r/§4${entity.maxHealth().round(1)} §c❤ ${winStatus(entity)}"
    }

    private fun statusString(entity: Entity): String {
        if (entity.isDead) return "§cDEAD"
        val returnString = StringBuilder()
        if (mc.thePlayer.canEntityBeSeen(entity) && entity is EntityPlayer) {
            try {
                if (entity.isBurning) returnString.append("§cFIRE ")
                if (entity.isBlocking) returnString.append("§eBLOCK ")
                if (entity.isEating && entity.heldItem?.item != null && entity.heldItem?.item is ItemAppleGold) returnString.append("§6GAP ")
                //if ((entity.itemInUse.item ?: returnString.append("")) is ItemBow) returnString.append("§fBOW ")
            } catch (e: Exception) {
                if (e is NullPointerException) devMessage("Caught a Null Pointer Exception firing statusString") else
                    devMessage("Caught an exception firing statusString")
                e.printStackTrace()
            }
        } else if (!mc.thePlayer.canEntityBeSeen(entity) && entity is EntityPlayer) return "§6NO SIGHT LINE"
        return returnString.toString()
    }

    /**@SubscribeEvent
    fun onDamage(event: LivingHurtEvent) {
        if (mc.thePlayer.canEntityBeSeen(event.entityLiving))
    } */

    init {
        execute(50) { if (targetCD.hasTimePassed()) target = null }

        onMessage(triggerRegex) {
            val (player, _) = triggerRegex.matchEntire(it)?.destructured ?: return@onMessage
            mc.theWorld.getPlayerEntityByName(player).let { entity ->
                target = entity
                targetCD.update()
            }
        }

    }
}