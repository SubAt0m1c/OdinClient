package me.odinclient.features.impl.m7

import me.odinclient.OdinClient.Companion.mc
import me.odinclient.features.Category
import me.odinclient.features.Module
import me.odinclient.features.settings.impl.NumberSetting
import me.odinclient.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos

object DioriteFucker : Module(
    "Fuck Diorite",
    category = Category.M7
) {
    private val delay: Long by NumberSetting("Delay", 80L, 20, 300, 10)

    init {
        executor(delay = { delay }) {
            if (mc.theWorld == null || DungeonUtils.getPhase() != 2) return@executor
            for (block in pillars) {
                if (mc.theWorld.chunkProvider.provideChunk(block.x shr 4, block.z shr 4).getBlock(block) == Blocks.stone) {
                    mc.theWorld.setBlockState(block, Blocks.glass.defaultState, 3)
                }
            }
        }
    }

    private val pillars = ArrayList<BlockPos>().apply {
        val basePillars = arrayListOf(
            BlockPos(45, 169, 44),
            BlockPos(46, 169, 44),
            BlockPos(47, 169, 44),
            BlockPos(44, 169, 43),
            BlockPos(45, 169, 43),
            BlockPos(46, 169, 43),
            BlockPos(47, 169, 43),
            BlockPos(48, 169, 43),
            BlockPos(43, 169, 42),
            BlockPos(44, 169, 42),
            BlockPos(45, 169, 42),
            BlockPos(46, 169, 42),
            BlockPos(47, 169, 42),
            BlockPos(48, 169, 42),
            BlockPos(49, 169, 42),
            BlockPos(43, 169, 41),
            BlockPos(44, 169, 41),
            BlockPos(45, 169, 41),
            BlockPos(46, 169, 41),
            BlockPos(47, 169, 41),
            BlockPos(48, 169, 41),
            BlockPos(49, 169, 41),
            BlockPos(43, 169, 40),
            BlockPos(44, 169, 40),
            BlockPos(45, 169, 40),
            BlockPos(46, 169, 40),
            BlockPos(47, 169, 40),
            BlockPos(48, 169, 40),
            BlockPos(49, 169, 40),
            BlockPos(44, 169, 39),
            BlockPos(45, 169, 39),
            BlockPos(46, 169, 39),
            BlockPos(47, 169, 39),
            BlockPos(48, 169, 39),
            BlockPos(45, 169, 38),
            BlockPos(46, 169, 38),
            BlockPos(47, 169, 38),

            BlockPos(45, 169, 68),
            BlockPos(46, 169, 68),
            BlockPos(47, 169, 68),
            BlockPos(44, 169, 67),
            BlockPos(45, 169, 67),
            BlockPos(46, 169, 67),
            BlockPos(47, 169, 67),
            BlockPos(48, 169, 67),
            BlockPos(43, 169, 66),
            BlockPos(44, 169, 66),
            BlockPos(45, 169, 66),
            BlockPos(46, 169, 66),
            BlockPos(47, 169, 66),
            BlockPos(48, 169, 66),
            BlockPos(49, 169, 66),
            BlockPos(43, 169, 65),
            BlockPos(44, 169, 65),
            BlockPos(45, 169, 65),
            BlockPos(46, 169, 65),
            BlockPos(47, 169, 65),
            BlockPos(48, 169, 65),
            BlockPos(49, 169, 65),
            BlockPos(43, 169, 64),
            BlockPos(44, 169, 64),
            BlockPos(45, 169, 64),
            BlockPos(46, 169, 64),
            BlockPos(47, 169, 64),
            BlockPos(48, 169, 64),
            BlockPos(49, 169, 64),
            BlockPos(44, 169, 63),
            BlockPos(45, 169, 63),
            BlockPos(46, 169, 63),
            BlockPos(47, 169, 63),
            BlockPos(48, 169, 63),
            BlockPos(45, 169, 62),
            BlockPos(46, 169, 62),
            BlockPos(47, 169, 62)
        )
        repeat(28) { height ->
            for (block in basePillars) {
                add(block.add(0, height, 0))
            }
        }
    }
}