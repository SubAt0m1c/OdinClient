package me.odinmain.features.impl.subaddons

import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.impl.subaddons.SubUtils.isPlayer
import me.odinmain.features.settings.impl.BooleanSetting
import me.odinmain.features.settings.impl.ColorSetting
import me.odinmain.features.settings.impl.HudSetting
import me.odinmain.ui.hud.HudElement
import me.odinmain.utils.clock.Clock
import net.minecraft.client.gui.Gui
import me.odinmain.utils.max
import me.odinmain.utils.render.Color
import me.odinmain.utils.render.RenderUtils.bind
import me.odinmain.utils.render.dropShadow
import me.odinmain.utils.render.mcText
import me.odinmain.utils.render.roundedRectangle
import me.odinmain.utils.round
import me.odinmain.utils.runIn
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderHelper

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityCreature
import net.minecraft.entity.EntityLiving
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.potion.Potion
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.entity.player.AttackEntityEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11

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
        if (target == null || targetEntity?.isEntityAlive == false) return@HudSetting 0f to 0f
        val entity = if (target?.entityLiving != null) target?.entityLiving else target?.playerEntity

        if (entity is EntityPlayer && target?.isPlayer == false) return@HudSetting 0f to 0f

        if (outline) {
            roundedRectangle(-1.5f, -1.5f, 53f, 41f, outlinecolor, outlinecolor, outlinecolor, 0f , 9f, 0f, 9f, 0f, 0f)
            roundedRectangle(51.5f, -1.5f, 50f, 41f, outlinecolor, outlinecolor, outlinecolor, 0f , 0f, 9f, 0f, 9f, 0f)
        }

        roundedRectangle(0f, 0f, 50f, 38f, color, color, color, 0f , 9f, 0f, 9f, 0f, 0f)
        roundedRectangle(50f, 0f, 50f, 38f, color, color, color, 0f , 0f, 9f, 0f, 9f, 0f)

        val health = entity?.health ?: 0f
        val maxHealth = entity?.maxHealth ?: 0f
        val colorHealth = when {
            health == maxHealth -> "§2${health.round(1)}"
            health >= (maxHealth * 0.75) -> "§a${health.round(1)}"
            health >= (maxHealth * 0.25) -> "§6${health.round(1)}"
            else -> "§c${health.round(1)}"
        }
        if (targetEntity != null) drawEntityFace(targetEntity, 8, 13,)
        mcText(target?.entity?.displayName?.unformattedText ?: "???", 25f, 5f, 1, Color.BLUE, center = false)
        mcText(if (targetEntity?.isEntityAlive == true) "$colorHealth§r/§4${maxHealth} §cHP" else "§cDead", 25f, 15f, 1, Color.WHITE, center = false)
        if (targetEntity != null && targetEntity.isEntityAlive) mcText("${if (targetEntity.isBurning) "§6Fire " else ""}${if (targetEntity is EntityPlayer && targetEntity.isBlocking) "§eBlock " else ""}${if (targetEntity.isSprinting) "§fSprint" else ""}", 25f, 25f, 1, Color.WHITE, center = false)
        50f to 38f
    }

    val targetCD = Clock(3_000)

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
            playerEntity
        )
    }

    /**@SubscribeEvent
    fun onDamage(event: LivingHurtEvent) {
        if (mc.thePlayer.canEntityBeSeen(event.entityLiving))
    } */

    init {
        execute(50) {
            if (targetCD.hasTimePassed()) target = null
        }
    }

    fun drawEntityFace(entity: Entity, x: Int, y: Int, ) {
        val playerInfo: NetworkPlayerInfo = mc.netHandler.getPlayerInfo(entity.uniqueID)?: return

        mc.textureManager.bindTexture(playerInfo.locationSkin)
        GL11.glColor4f(1F, 1F, 1F, 1F);

        Gui.drawScaledCustomSizeModalRect(x-5, y-5, 8f, 8f, 8,8,20,20,64f, 64f )
    }
}