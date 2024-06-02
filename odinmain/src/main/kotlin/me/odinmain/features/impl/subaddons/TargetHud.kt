package me.odinmain.features.impl.subaddons

import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.impl.subaddons.nofeature.SubRenderUtils.drawHealthBarInWorld
import me.odinmain.features.impl.subaddons.nofeature.SubRenderUtils.drawEntityFace
import me.odinmain.features.impl.subaddons.nofeature.SubRenderUtils.drawHealthBar
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.health
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.healthColor
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.healthPercent
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.healthColorCode
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
import net.minecraft.entity.EntityCreature
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
    private val healthBar: Boolean by BooleanSetting("Health bar", description = "displays a health bar next to the target")
    private val style: Boolean by DualSetting("Style", left = "scale", right = "Object", default = false).withDependency { healthBar }
    private val expand: Double by NumberSetting("Expand", 0.0, -0.3, 2.0, 0.1, description = "Expand the health bar").withDependency { healthBar && style}
    private val xShift: Double by NumberSetting("X Shift", 0.0, -35, 10, 1.0, description = "Offset the X of the health bar").withDependency { healthBar && style}
    //private val allBar: Boolean by BooleanSetting("Everyone", description = "Shows the health bar on everyone, regardless of whether or not theyre your target").withDependency { healthBar }

    private val hud: HudElement by HudSetting("Display", 10f, 10f, 2f, false) {
        //if (followPlayer) return@HudSetting 0f to 0f
        val targetEntity = target?.entity
        if (it) {
            val round: Float = if (rounded) 9f else 0f
            if (outline) roundedRectangle(-2f, -2f, 100 + 4f, 42f, outlinecolor, outlinecolor, outlinecolor, 0f , round, round, round, round, 0.5f)
            roundedRectangle(0f, 0f, 100f, 38f, color, color, color, 0f , round, round, round, round, 0.5f)
        } else if (target != null && targetEntity?.isInvisible == false) {
            if (targetEntity is EntityPlayer && target?.isPlayer == false) return@HudSetting 0f to 0f
            val start = if (targetEntity is EntityPlayer) 30f else 5f
            val width = max(getMCTextWidth(target?.entity?.displayName?.unformattedText ?: "???"), getMCTextWidth(healthString(targetEntity)), getMCTextWidth(statusString(targetEntity))) + start + 5f
            val round: Float = if (rounded) 9f else 0f

            if (outline) roundedRectangle(-2f, -2f, width + 4f, 42f, outlinecolor, outlinecolor, outlinecolor, 0f , round, round, round, round, 0.5f)

            val hp = targetEntity.healthPercent()
            val healthWidth = width * min(hp, 1f)
            val abHealthWidth = width * if (hp > 1) ((hp - 1)%(1)) else 0.0
            val ugh = if (rounded) 10f else 5f
            val ugh2 = if (rounded) 28f else 33f
            if (hp < 1) roundedRectangle(0f, ugh2, width, ugh, Color.DARK_GRAY, Color.DARK_GRAY, Color.DARK_GRAY, 0f , 0f, 0f, round, round, 0.5f)
            roundedRectangle(0f, ugh2, healthWidth, ugh, targetEntity.healthColor(), targetEntity.healthColor(), targetEntity.healthColor(), 0f , 0f, 0f, round, round, 0.5f)
            if (hp > 1) roundedRectangle((width - abHealthWidth), ugh2, abHealthWidth, ugh, Color.BLUE, Color.BLUE, Color.BLUE, 0f , 0f, 0f, round, round, 0.5f)

            val color2: Color = if (rounded) Color(color.r, color.g, color.b, 1f) else color
            roundedRectangle(0f, 0f, width, 33, color2, color2, color2, 0f , round, round, 0, 0, 0.5f)

            if (targetEntity is EntityPlayer) drawEntityFace(targetEntity, 8, 10, 1.2f)
            mcText(target?.entity?.displayName?.unformattedText ?: "???", start, 5f, 1, Color.BLUE, center = false)
            mcText(healthString(targetEntity), start, 15f, 1, Color.WHITE, center = false)
            mcText(statusString(targetEntity), start, 25f, 1, Color.WHITE, center = false)
        }
        100f to 38f
    }

    private val targetCD = Clock(3_000)
    private val triggerRegex = Regex("^(\\w{1,16}) is on (\\d*\\.?\\d*) HP!\$")

    var target: targetEntity? = null
    data class targetEntity(
        val entity: Entity,
        val isPlayer: Boolean,
        val entityLiving: EntityLiving?,
        val playerEntity: EntityPlayer?
    )

    @SubscribeEvent
    fun onAttack(event: AttackEntityEvent){
        val entity = mc.theWorld.getEntityByID(event.target.entityId) ?: return
        val playerEntity = if (entity.isEntityAlive && entity is EntityPlayer) entity else null
        val livingEntity = if (entity.isEntityAlive && entity is EntityCreature) entity else null
        if ((playerEntity == null && livingEntity == null) || entity.isInvisible) return

        target = targetEntity(
            entity,
            entity.isPlayer(),
            livingEntity,
            playerEntity
        )
        targetCD.update()
    }


    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (target?.entity?.isDead == true && !targetCD.hasTimePassed()) {
            val targetEntity = target?.entity ?: return
            runIn(15) { if (!targetCD.hasTimePassed() && target?.entity == targetEntity) target = null }
        }

        val entity = mc.pointedEntity
        if (targetCD.hasTimePassed() && target != null) target = null

        if (entity != null) {
            val playerEntity = if (entity.isEntityAlive && entity is EntityPlayer) entity else null
            val livingEntity = if (entity.isEntityAlive && entity is EntityCreature) entity else null
            if ((playerEntity != null || livingEntity != null) && !entity.isInvisible) {
                target = targetEntity(
                    entity,
                    entity.isPlayer(),
                    livingEntity,
                    playerEntity,
                )
            }
        }

        if (
            healthBar && target?.entity != null &&
            mc.thePlayer.canEntityBeSeen(target?.entity) &&
            target?.entity?.isInvisible == false
            && (target?.isPlayer == true || entity !is EntityPlayer)
            )  {
            if (!style) drawHealthBar(target?.entity ?: return) else
                drawHealthBarInWorld(target?.entity ?: return, expand, xShift)
        }
        //if (followPlayer && target?.entity != null) drawTargetHudInWorld(target?.entity ?: return, outline, outlinecolor, color)
    }

    private fun healthString(entity: Entity): String {
        val health = entity.health()
        val maxHealth = entity.maxHealth()
        val colorHealth = "${entity.healthColorCode()}${health.round(1)}"

        val winStatus = when {
            health > mc.thePlayer.health -> "§l§cL"
            health == mc.thePlayer.health -> "§3T"
            else -> "§l§aW"
        }

        return "$colorHealth§r/§4${maxHealth.round(1)} §c❤ $winStatus"
    }

    private fun statusString(entity: Entity): String {
        if (entity.isDead) return "§cDEAD"
        val returnString = StringBuilder()
        if (mc.thePlayer.canEntityBeSeen(entity) && entity is EntityPlayer) {
            try {
                if (entity.isBurning) returnString.append("§cFIRE ")
                if (entity.isBlocking) returnString.append("§eBLOCK ")
                if (entity.isEating && entity.heldItem.item is ItemAppleGold && entity.heldItem.item != null) returnString.append("§6GAP ")
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
        execute(50) {
            if (targetCD.hasTimePassed()) target = null
        }

        onMessage(triggerRegex) {
            val (player, _) = triggerRegex.matchEntire(it)?.destructured ?: return@onMessage
            mc.theWorld.getPlayerEntityByName(player).let { name ->
                target = targetEntity(name, name.isPlayer(), null, name)
                targetCD.update()
            }
        }

    }
}