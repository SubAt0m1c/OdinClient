
package me.odinmain.features.impl.subaddons

import me.odinmain.OdinMain.isLegitVersion
import me.odinmain.events.impl.PostEntityMetadata
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.settings.Setting.Companion.withDependency
import me.odinmain.features.settings.impl.*
import me.odinmain.utils.getPositionEyes
import me.odinmain.utils.profile
import me.odinmain.utils.render.RenderUtils.renderVec
import me.odinmain.utils.skyblock.LocationUtils.inSkyblock
import me.odinmain.features.impl.subaddons.SubUtils.isOnTeam
import me.odinmain.features.impl.subaddons.SubUtils.isPlayer
import me.odinmain.utils.render.*
import me.odinmain.utils.render.HighlightRenderer.highlightModeDefault
import me.odinmain.utils.render.HighlightRenderer.highlightModeList
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
    private val mode: Int by SelectorSetting("Mode", highlightModeDefault, highlightModeList)
    //val mode: Int by SelectorSetting("Mode", "Outline", arrayListOf("Outline", "Overlay", "Boxes", "2D"))
    val thickness: Float by NumberSetting("Line Width", 5f, .5f, 20f, .1f, description = "The line width of Outline/ Boxes/ 2D Boxes").withDependency { mode != HighlightRenderer.HighlightType.Overlay.ordinal }
    private val glowIntensity: Float by NumberSetting("Glow Intensity", 2f, .5f, 5f, .1f, description = "The intensity of the glow effect.").withDependency { mode == HighlightRenderer.HighlightType.Glow.ordinal }
    private val tracerLimit: Int by NumberSetting("Tracer Limit", 0, -1, 15, description = "Highlight will draw tracer to all players when you have under this amount of players marked, set to 0 to disable, set to -1 for infinite.").withDependency { !isLegitVersion }
    //the way these tracers work without xray, it might actually still be legit according to SkyHanni. Not sure how but /shrug
    private val xray: Boolean by BooleanSetting("Through Walls", true).withDependency { !isLegitVersion }
    private val showinvis: Boolean by BooleanSetting("Show Invis", false).withDependency { !isLegitVersion }
    private val advanced: Boolean by DropdownSetting("Show Settings", false)
    private val teamColor: Color by ColorSetting("Team Color", Color.CYAN, true).withDependency { advanced }
    private val oppColor: Color by ColorSetting("Opponent Color", Color.RED, true).withDependency { advanced }
    val color: Color by ColorSetting("Backup Color", Color.ORANGE, true).withDependency { advanced }

    private val renderThrough: Boolean get() = if (isLegitVersion) false else xray

    private var currentplayers = mutableSetOf<Entity>()

    private fun getDisplayColor(entity: Entity): Color {
        val displayColor = when {
            entity.isInvisible && !showinvis -> Color.TRANSPARENT
            !entity.isOnTeam()-> oppColor
            entity.isOnTeam() -> teamColor
            else -> color
        }
        return displayColor
    }

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

        HighlightRenderer.addEntityGetter({ HighlightRenderer.HighlightType.entries[mode]}) {
            if (!enabled) emptyList()
            else currentplayers.map { HighlightRenderer.HighlightEntity(it, getDisplayColor(it), thickness, !renderThrough, glowIntensity) }
        }
    }

    /** @SubscribeEvent
    fun onRenderEntityModel(event: RenderEntityModelEvent) {
        if (mode != 0 || event.entity !in currentplayers || (!mc.thePlayer.canEntityBeSeen(event.entity) && !renderThrough)) return
        if (!event.entity.isInvisible || showinvis) profile("Outline Esp") { OutlineUtils.outlineEntity(event, thickness, getDisplayColor(event.entity), cancelHurt) }
    } */

    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent) {
        val tracerPlayers = currentplayers.filter { (renderThrough || (mc.thePlayer.canEntityBeSeen(it) && !isLegitVersion) ) && (!it.isInvisible || showinvis) }
        profile("tracers") {tracerPlayers.forEach {
            if (tracerPlayers.size < tracerLimit || tracerLimit == -1)
                RenderUtils.draw3DLine(getPositionEyes(mc.thePlayer.renderVec), getPositionEyes(it.renderVec), getDisplayColor(it),
                    2F, false)
        }}

        /** profile("ESP") { currentplayers.forEach {
            if (mode == 2 && (!it.isInvisible || showinvis))
                Renderer.drawBox(it.entityBoundingBox, getDisplayColor(it), thickness, depth = !renderThrough, fillAlpha = 0)
            else if (mode == 3 && (mc.thePlayer.canEntityBeSeen(it) || renderThrough) && (!it.isInvisible || showinvis))
                Renderer.draw2DEntity(it, thickness, getDisplayColor(it))
        }} */
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
        if (!inSkyblock && entity.isPlayer() && entity != mc.thePlayer) currentplayers.add(entity)
    }
}


