package me.odinmain.features.impl.subaddons

import me.odinmain.OdinMain
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.drawEntityFace
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.health
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.isPlayer
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.maxHealth
import me.odinmain.features.settings.impl.BooleanSetting
import me.odinmain.features.settings.impl.ColorSetting
import me.odinmain.features.settings.impl.HudSetting
import me.odinmain.ui.hud.HudElement
import me.odinmain.utils.*
import me.odinmain.utils.clock.Clock
import me.odinmain.utils.render.Color
import me.odinmain.utils.render.mcText
import me.odinmain.utils.render.roundedRectangle
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
    private val color: Color by ColorSetting("Color", Color(21, 22, 23, 0.5f), allowAlpha = true)
    private val outlinecolor: Color by ColorSetting("Outline Color", Color(21, 22, 23, 0.5f), allowAlpha = true)

    private val outline: Boolean by BooleanSetting("Outline", true)
    private val hud: HudElement by HudSetting("Display", 10f, 10f, 2f, false) {
        val targetEntity = target?.entity
        if (target != null && targetEntity?.isInvisible == false) {
            if (targetEntity is EntityPlayer && target?.isPlayer == false) return@HudSetting 0f to 0f

            if (outline) {
                roundedRectangle(-2f, -2f, 54f, 42f, outlinecolor, outlinecolor, outlinecolor, 0f , 9f, 0f, 9f, 0f, 0f)
                roundedRectangle(52f, -2f, 50f, 42f, outlinecolor, outlinecolor, outlinecolor, 0f , 0f, 9f, 0f, 9f, 0f)
            }

            roundedRectangle(0f, 0f, 50f, 38f, color, color, color, 0f , 9f, 0f, 9f, 0f, 0f)
            roundedRectangle(49.9f, 0f, 50f, 38f, color, color, color, 0f , 0f, 9f, 0f, 9f, 0f) //Why does it need to be 49.9???

            drawEntityFace(targetEntity, 8, 13,)
            mcText(target?.entity?.displayName?.unformattedText ?: "???", 25f, 5f, 1, Color.BLUE, center = false)
            mcText(healthString(targetEntity), 25f, 15f, 1, Color.WHITE, center = false)
            mcText(statusString(targetEntity), 25f, 25f, 1, Color.WHITE, center = false)
        } else return@HudSetting 0f to 0f
        50f to 38f
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
        if (playerEntity == null && livingEntity == null) return

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
            runIn(15) {
                if (!targetCD.hasTimePassed() && target?.entity == targetEntity) target = null
            }
        }

        val entity = mc.pointedEntity ?: return
        if (targetCD.hasTimePassed() && target != null) target = null

        val playerEntity = if (entity.isEntityAlive && entity is EntityPlayer) entity else null
        val livingEntity = if (entity.isEntityAlive && entity is EntityCreature) entity else null
        if (playerEntity == null && livingEntity == null) return

        target = targetEntity(
            entity,
            entity.isPlayer(),
            livingEntity,
            playerEntity,
        )
    }

    private fun healthString(entity: Entity): String {
        val health = entity.health()
        val maxHealth = entity.maxHealth()

        val colorHealth = when {
            health == maxHealth -> "§2${health.round(1)}"
            health >= (maxHealth * 0.75) -> "§a${health.round(1)}"
            health >= (maxHealth * 0.25) -> "§6${health.round(1)}"
            else -> "§c${health.round(1)}"
        }

        return "$colorHealth§r/§4${maxHealth} §cHP"
    }

    private fun statusString(entity: Entity): String {
        if (entity.isDead) return "§cDEAD"
        val returnString = StringBuilder()
        if (mc.thePlayer.canEntityBeSeen(entity) && entity is EntityPlayer) {
            if (entity.isBurning) returnString.append("§cFIRE ")
            if (entity.isBlocking) returnString.append("§eBLOCK ")
            if (entity.isEating && entity.heldItem.item != null && entity.heldItem.item is ItemAppleGold && entity.heldItem.item != null) returnString.append("§6GAP ")
            //if ((entity.itemInUse.item ?: returnString.append("")) is ItemBow) returnString.append("§fBOW ")
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