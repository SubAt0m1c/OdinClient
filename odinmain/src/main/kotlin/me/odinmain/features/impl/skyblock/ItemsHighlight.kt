package me.odinmain.features.impl.skyblock

import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.settings.impl.BooleanSetting
import me.odinmain.features.settings.impl.DualSetting
import me.odinmain.features.settings.impl.NumberSetting
import me.odinmain.features.settings.impl.SelectorSetting
import me.odinmain.utils.render.Color
import me.odinmain.utils.render.HighlightRenderer
import me.odinmain.utils.render.HighlightRenderer.highlightModeDefault
import me.odinmain.utils.render.HighlightRenderer.highlightModeList
import me.odinmain.utils.skyblock.LocationUtils
import me.odinmain.utils.skyblock.getRarity
import me.odinmain.utils.skyblock.lore
import net.minecraft.entity.item.EntityItem

object ItemsHighlight : Module(
    "Item Highlight",
    description = "Outlines dropped item entities.",
    category = Category.RENDER
) {
    private val renderThrough: Boolean by BooleanSetting("Through Walls", true)
    private val colorStyle: Boolean by DualSetting("Color Style", "Rarity", "Distance", default = false, description = "Which color style to use")
    private val mode: Int by SelectorSetting("Mode", highlightModeDefault, highlightModeList)
    private val thickness: Float by NumberSetting("Line Width", 0.5f, 0.2f, 1f, .1f, description = "The line width of Outline / Boxes/ 2D Boxes")

    init {
        HighlightRenderer.addEntityGetter({ HighlightRenderer.HighlightType.entries[mode]}) {
            if (!enabled || !LocationUtils.inSkyblock) emptyList()
            else mc.theWorld.loadedEntityList.filterIsInstance<EntityItem>().map {
                HighlightRenderer.HighlightEntity(it, getEntityOutlineColor(it), thickness, !renderThrough, 1f)
            }
        }
    }

    private fun getEntityOutlineColor(entity: EntityItem): Color {
        return if (!colorStyle) getRarity(entity.entityItem.lore)?.color ?: Color.WHITE
        else if (entity.getDistanceToEntity(mc.thePlayer) <= 3.5) {
            Color.GREEN
        } else {
            Color.RED
        }
    }
}