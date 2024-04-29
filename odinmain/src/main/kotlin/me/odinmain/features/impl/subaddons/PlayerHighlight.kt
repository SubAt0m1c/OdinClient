
package me.odinmain.features.impl.subaddons

import me.odinmain.OdinMain.isLegitVersion
import me.odinmain.events.impl.PostEntityMetadata
import me.odinmain.events.impl.RenderEntityModelEvent
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.impl.subaddons.SubUtils.isIt
import me.odinmain.features.settings.Setting.Companion.withDependency
import me.odinmain.features.settings.impl.*
import me.odinmain.utils.ServerUtils.getPing
import me.odinmain.utils.equalsOneOf
import me.odinmain.utils.getPositionEyes
import me.odinmain.utils.profile
import me.odinmain.utils.render.Color
import me.odinmain.utils.render.OutlineUtils
import me.odinmain.utils.render.RenderUtils
import me.odinmain.utils.render.RenderUtils.renderVec
import me.odinmain.utils.render.Renderer
import me.odinmain.utils.skyblock.LocationUtils.inSkyblock
import me.odinmain.features.impl.subaddons.SubUtils.isOnTeam
import net.minecraft.entity.Entity
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


object PlayerHighlight : Module(
    name = "Player Highlight",
    category = Category.SUBADDONS,
    tag = TagType.FPSTAX,
    description = "Highlights players"
) {
    private val scanDelay: Long by NumberSetting("Scan Delay", 500L, 10L, 2000L, 100L)
    val mode: Int by SelectorSetting("Mode", "Outline", arrayListOf("Outline", "Overlay", "Boxes", "2D"))
    val thickness: Float by NumberSetting("Line Width", 5f, .5f, 20f, .1f, description = "The line width of Outline/ Boxes/ 2D Boxes").withDependency { mode.equalsOneOf(0, 2, 3) }
    private val tracerLimit: Int by NumberSetting("Tracer Limit", 0, 0, 15, description = "Highlight will draw tracer to all mobs when you have under this amount of mobs marked, set to 0 to disable. Helpful for finding lost mobs.").withDependency { !isLegitVersion }

    private val xray: Boolean by BooleanSetting("Through Walls", true).withDependency { !isLegitVersion }
    private val showinvis: Boolean by BooleanSetting("Show Invis", false).withDependency { !isLegitVersion }
    private val cancelHurt: Boolean by BooleanSetting("Cancel Hurt", true).withDependency { mode != 1 }
    private val advanced: Boolean by DropdownSetting("Show Settings", false)
    private val teamColor: Color by ColorSetting("Team Color", Color.CYAN, true).withDependency { advanced }
    private val oppColor: Color by ColorSetting("Opponent Color", Color.RED, true).withDependency { advanced }
    val color: Color by ColorSetting("Backup Color", Color.ORANGE, true).withDependency { advanced }

    private val renderThrough: Boolean get() = if (isLegitVersion) false else xray

    private var currentplayers = mutableSetOf<Entity>()

    init {
        execute({ scanDelay }) {
            currentplayers.removeAll { it.isDead }
            getEntities()
        }

        execute(30_000) {
            currentplayers.clear()
            getEntities()
        }

        onWorldLoad { currentplayers.clear() }
    }

    @SubscribeEvent
    fun onRenderEntityModel(event: RenderEntityModelEvent) {
        if (mode != 0 || event.entity !in currentplayers || (!mc.thePlayer.canEntityBeSeen(event.entity) && !renderThrough)) return

        val displayColor = when {
            event.entity.isIt() && event.entity.getPing() == 1 -> color
            !event.entity.isOnTeam() && event.entity.getPing() == 1 -> oppColor
            event.entity.isOnTeam() && event.entity.getPing() == 1 -> teamColor
            else -> color
        }
        if (!event.entity.isInvisible || showinvis) profile("Outline Esp") { OutlineUtils.outlineEntity(event, thickness, displayColor, cancelHurt) }
    }

    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent) {
        val tracerPlayers = currentplayers.filter { (renderThrough || (mc.thePlayer.canEntityBeSeen(it) && !isLegitVersion) ) && (!it.isInvisible || showinvis) }
        profile("tracers") {tracerPlayers.forEach {

            val displayColor = when {
                it.isIt() && it.getPing() == 1 -> color
                !it.isOnTeam() && it.getPing() == 1 -> oppColor
                it.isOnTeam() && it.getPing() == 1 -> teamColor
                else -> color
            }

            if (tracerPlayers.size < tracerLimit)
                RenderUtils.draw3DLine(getPositionEyes(mc.thePlayer.renderVec), getPositionEyes(it.renderVec), displayColor,
                    2F, false)
        }}

        profile("ESP") { currentplayers.forEach {

            val displayColor = when {
                !it.isOnTeam() && it.getPing() == 1 -> oppColor
                it.isOnTeam() && it.getPing() == 1 -> teamColor
                else -> color
            }

            if (mode == 2 && (!it.isInvisible || showinvis))
                Renderer.drawBox(it.entityBoundingBox, displayColor, thickness, depth = !renderThrough, fillAlpha = 0)
            else if (mode == 3 && (mc.thePlayer.canEntityBeSeen(it) || renderThrough) && (!it.isInvisible || showinvis))
                Renderer.draw2DEntity(it, thickness, displayColor)
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
        if (!inSkyblock && entity.getPing() == 1 && entity != mc.thePlayer) currentplayers.add(entity)
    }
}


