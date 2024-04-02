package me.odinmain.features.impl.dungeon

import codes.som.anthony.koffee.modifiers.private
import me.odinmain.OdinMain
import me.odinmain.events.impl.RenderEntityModelEvent
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.impl.render.CustomHighlight
import me.odinmain.features.settings.Setting.Companion.withDependency
import me.odinmain.features.settings.impl.BooleanSetting
import me.odinmain.features.settings.impl.ColorSetting
import me.odinmain.features.settings.impl.NumberSetting
import me.odinmain.features.settings.impl.SelectorSetting
import me.odinmain.ui.util.shader.FramebufferShader
import me.odinmain.ui.util.shader.GlowShader
import me.odinmain.ui.util.shader.OutlineShader
import me.odinmain.utils.addVec
import me.odinmain.utils.equalsOneOf
import me.odinmain.utils.getPositionEyes
import me.odinmain.utils.profile
import me.odinmain.utils.render.Color
import me.odinmain.utils.render.OutlineUtils
import me.odinmain.utils.render.RenderUtils
import me.odinmain.utils.render.RenderUtils.createLegacyShader
import me.odinmain.utils.render.RenderUtils.renderBoundingBox
import me.odinmain.utils.render.RenderUtils.renderVec
import me.odinmain.utils.render.Renderer
import me.odinmain.utils.skyblock.dungeon.DungeonUtils
import me.odinmain.utils.skyblock.dungeon.DungeonUtils.dungeonTeammatesNoSelf
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11

object TeammatesHighlight : Module(
    "Teammate Highlight",
    category = Category.DUNGEON,
    description = "Enhances visibility of your dungeon teammates and their name tags."
) {
    private val showClass: Boolean by BooleanSetting("Show Class", true, description = "Shows the class of the teammate.")
    private val mode: Int by SelectorSetting("Mode", "Outline", arrayListOf("Outline", "boxes", "2D"))
    // private val color: Color by ColorSetting("Color", Color.CYAN, true).withDependency { mode.equalsOneOf(0, 1, 2, 3, 4) }
    private val thickness: Float by NumberSetting("Line Width", 4f, 1.0, 10.0, 0.5, description = "The line width of Outline/ Boxes/ 2D Boxes")
    // private val glowIntensity: Float by NumberSetting("Glow Intensity", 2f, .5f, 5f, .1f, description = "The intensity of the glow effect.").withDependency { mode == 4 }
    private val visibility: Int by SelectorSetting("visability", "always visable", arrayListOf("Only When Visible", "Only when not Visible", "Always Visible"))
    private val inBoss: Boolean by BooleanSetting("In Boss", true, description = "Highlights teammates in boss rooms.")


    @SubscribeEvent
    fun onRenderEntityModel(event: RenderEntityModelEvent) {
        if (!DungeonUtils.inDungeons || (!inBoss && DungeonUtils.inBoss) || mode != 0) return

        val teammates = when (visibility) {
            0 -> {
                dungeonTeammatesNoSelf.filter {mc.thePlayer.canEntityBeSeen(it.entity)}
            }
            1 -> {
                dungeonTeammatesNoSelf.filter { !mc.thePlayer.canEntityBeSeen(it.entity) }
            }
            else -> {
                dungeonTeammatesNoSelf
            }
        }

        val teammate = teammates.find { it.entity == event.entity } ?: return

        profile("Highlight Dungeon Teammates") { OutlineUtils.outlineEntity(event, thickness, teammate.clazz.color, true) }
    }

    /** @SubscribeEvent
    fun on2d(event: RenderGameOverlayEvent.Pre) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR || !mode.equalsOneOf(3, 4) || !DungeonUtils.inDungeons || (!inBoss && DungeonUtils.inBoss)) return
        if (mode == 3) OutlineShader.startDraw(RenderUtils.partialTicks)
        else GlowShader.startDraw(RenderUtils.partialTicks)

        val teammates = if (whenVisible) {
            dungeonTeammatesNoSelf.filter {mc.thePlayer.canEntityBeSeen(it.entity)}
        } else {
            dungeonTeammatesNoSelf
        }

        teammates.forEach {
            mc.renderManager.renderEntityStatic(it.entity, RenderUtils.partialTicks, true)
        }

        if (mode == 4) OutlineShader.stopDraw(color, thickness / 3f, 1f)
        else GlowShader.endDraw(color, thickness, glowIntensity)
    } */

    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent) {
        if (!mode.equalsOneOf(1,2)) return

        val teammates = when (visibility) {
            0 -> {
                dungeonTeammatesNoSelf.filter {mc.thePlayer.canEntityBeSeen(it.entity)}
            }
            1 -> {
                dungeonTeammatesNoSelf.filter { !mc.thePlayer.canEntityBeSeen(it.entity) }
            }
            else -> {
                dungeonTeammatesNoSelf
            }
        }

        profile("ESP") { teammates.forEach {
            if (mode == 1)
                it.entity?.let { it1 ->
                    Renderer.drawBox(
                        it1.renderBoundingBox,
                        it.clazz.color,
                        thickness, fillAlpha = 0)
                }
            else if (mode == 2)
                it.entity?.let { it1 -> Renderer.draw2DEntity(it1, thickness, it.clazz.color) }
        }}
    }

    @SubscribeEvent
    fun handleNames(event: RenderLivingEvent.Post<*>) {
        if (!DungeonUtils.inDungeons) return
        val teammate = dungeonTeammatesNoSelf.find { it.entity == event.entity } ?: return

        Renderer.drawStringInWorld(
            if (showClass) "${teammate.name} §e[${teammate.clazz.name[0]}]" else teammate.name,
            event.entity.renderVec.addVec(y = 2.6),
            color = teammate.clazz.color,
            depth = false, renderBlackBox = false,
            scale = 0.05f
        )
    }
}