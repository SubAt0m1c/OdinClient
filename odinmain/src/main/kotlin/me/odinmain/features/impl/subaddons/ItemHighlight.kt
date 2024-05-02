package me.odinmain.features.impl.subaddons

import me.odinmain.OdinMain.isLegitVersion
import me.odinmain.events.impl.PostEntityMetadata
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.impl.render.CustomHighlight.thickness
import me.odinmain.features.settings.Setting.Companion.withDependency
import me.odinmain.features.settings.impl.BooleanSetting
import me.odinmain.features.settings.impl.SelectorSetting
import me.odinmain.utils.profile
import me.odinmain.utils.render.*
import me.odinmain.utils.skyblock.LocationUtils.inSkyblock
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ItemHighlight : Module(
    "Item Highlight",
    description = "Outlines dropped item entities.",
    category = Category.SUBADDONS
) {
    private val mode: Int by SelectorSetting("Mode", "Boxes", arrayListOf("Boxes", "2D"))
    private val xray: Boolean by BooleanSetting("Through Walls", true).withDependency { !isLegitVersion }

    private var currentitems = mutableSetOf<Entity>()

    private val renderThrough: Boolean get() = if (isLegitVersion) false else xray

    init {
        execute({ 500L }) {
            currentitems.removeAll { it.isDead }
            getEntities()
        }

        execute(30_000) {
            currentitems.clear()
            getEntities()
        }

        onWorldLoad { currentitems.clear() }
    }

    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent) {
        profile("ESP") { currentitems.forEach {
            if (it !is EntityItem) return@forEach
            if (mode == 0)
                Renderer.drawBox(it.entityBoundingBox, getEntityOutlineColor(it), thickness, depth = !renderThrough, fillAlpha = 0)
            else if (mode == 1 && (mc.thePlayer.canEntityBeSeen(it) || renderThrough))
                Renderer.draw2DEntity(it, thickness, getEntityOutlineColor(it))
        }}
    }

    @SubscribeEvent
    fun postMeta(event: PostEntityMetadata) {
        val entity = mc.theWorld.getEntityByID(event.packet.entityId) ?: return
        checkEntity(entity)
    }

    private fun getEntities() {
        mc.theWorld?.loadedEntityList?.forEach {
            checkEntity(it)
        }
    }

    private fun checkEntity(entity: Entity) {
        if (inSkyblock && entity is EntityItem) currentitems.add(entity)
    }

    private fun getEntityOutlineColor(entity: EntityItem): Color {
        return if (entity.getDistanceToEntity(mc.thePlayer) <= 3.5) {
            Color.GREEN
        } else {
            Color.RED
        }
    }
}