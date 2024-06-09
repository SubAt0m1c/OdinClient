package me.odinmain.features.impl.subaddons

import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.impl.subaddons.AntiCheat.CheatType.LegitScaffold.getName
import me.odinmain.features.impl.subaddons.nofeature.PlayerData
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.isOnTeam
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.isPlayer
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.overVoid
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.subMessage
import me.odinmain.features.settings.impl.NumberSetting
import me.odinmain.utils.skyblock.PlayerUtils.ClickType
import me.odinmain.utils.skyblock.modMessage
import net.minecraft.block.BlockAir
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.event.ClickEvent
import net.minecraft.item.ItemBlock
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraft.util.IChatComponent
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import java.util.*
import kotlin.math.abs


object AntiCheat: Module(
    name = "AntiCheat",
    category = Category.SUBADDONS,
    description = "Alerts you of cheaters."
) {
    private val interval: Double by NumberSetting("Detection Interval", 20.0, 0.0, 60.0, 1.0, description = "how many times to check per second")

    sealed class CheatType {
        data object AutoBlock : CheatType()
        data object NoFall : CheatType()
        data object NoSlow : CheatType()
        data object Scaffold : CheatType()
        data object LegitScaffold : CheatType()

        fun getName() : String {
            return when (this) {
                is AutoBlock -> "Auto Block"
                is NoFall -> "No Fall"
                is NoSlow -> "No Slow"
                is Scaffold -> "Scaffold"
                is LegitScaffold -> "Legit Scaffold"
            }
        }
    }

    private val flags = hashMapOf<UUID, HashMap<CheatType, Long>>()
    private val players = hashMapOf<UUID, PlayerData>()
    private var lastAlert = 0L

    fun alert(e: EntityPlayer, cheatType: CheatType) {
        //if (e.isOnTeam()) return
        val time = System.currentTimeMillis()
        if (interval > 0.0) {
            var hashmap = flags[e.uniqueID]
            if (hashmap == null) {
                hashmap = HashMap()
            } else {
                val n: Long? = hashmap[cheatType]
                if (n != null && abs(n - System.currentTimeMillis()) >= interval * 1000) return
            }
            hashmap[cheatType] = time
            flags[e.uniqueID] = hashmap
        }
        val chatComponentText = ChatComponentText("§eSubAddons §8»§r" + (e.displayName.unformattedText) + " §7detected for §d" + cheatType.getName())
        val chatStyle = ChatStyle()
        chatStyle.setChatClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/wdr " + e.name))
        (chatComponentText as IChatComponent).appendSibling(ChatComponentText(" §7[§cWDR§7]")).setChatStyle(chatStyle)
        mc.thePlayer.addChatMessage(chatComponentText)
        if (abs(lastAlert - System.currentTimeMillis()) >= 1500L) {
            mc.thePlayer.playSound("note.pling", 1.0f, 1.0f)
            lastAlert = time
        }
    }

    private fun onaClientTick() {
        if (mc.theWorld == null || mc.isSingleplayer) return
        for (entityPlayer in mc.theWorld.playerEntities) {
            if (entityPlayer == null) continue
            if (entityPlayer == mc.thePlayer) continue
            if (!entityPlayer.isPlayer()) continue
            var data = players[entityPlayer.uniqueID]
            if (data == null) data = PlayerData()
            data.update(entityPlayer)
            performCheck(entityPlayer, data)
            data.updateServerPos(entityPlayer)
            data.updateSneak(entityPlayer)
            players[entityPlayer.uniqueID] = data
        }
    }

    init {
        onWorldLoad {
            players.clear()
            flags.clear()
        }

        onClientTick {
            onaClientTick()
        }
    }

    private fun performCheck(entityPlayer: EntityPlayer, playerData: PlayerData) {
        if (playerData.autoBlockTicks >= 10) return alert(entityPlayer, CheatType.AutoBlock)
        if (playerData.sneakTicks >= 3) return alert(entityPlayer, CheatType.LegitScaffold)
        if (playerData.noSlowTicks >= 11 && playerData.speed >= 0.08) alert(entityPlayer, CheatType.NoSlow)
        if (entityPlayer.isSwingInProgress &&
            entityPlayer.rotationPitch >= 70.0f &&
            entityPlayer.heldItem != null &&
            entityPlayer.heldItem.item is ItemBlock &&
            playerData.fastTick >= 20 &&
            entityPlayer.ticksExisted - playerData.lastSneakTick >= 30
            && entityPlayer.ticksExisted - playerData.aboveVoidTicks >= 20)
        {
            var overAir = true
            var blockPos = entityPlayer.position.down(2)
            for (i in 0..3) {
                if (mc.theWorld.getBlockState(blockPos).block !is BlockAir) {
                    overAir = false
                    break
                }
                blockPos = blockPos.down()
            }
            if (overAir) return alert(entityPlayer, CheatType.Scaffold)
        }
        if (!entityPlayer.capabilities.isFlying) {
            val serverPos = Vec3(
                entityPlayer.serverPosX / 32.0,
                entityPlayer.serverPosY / 32.0,
                entityPlayer.serverPosZ / 32.0
            )
            val delta = Vec3(
                abs(playerData.serverPosX - serverPos.xCoord),
                playerData.serverPosY - serverPos.yCoord,
                abs(playerData.serverPosZ - serverPos.zCoord),
            )
            if (delta.yCoord >= 5 && delta.xCoord <= 10 && delta.zCoord <= 10 && delta.yCoord <=40) {
                if (!overVoid(serverPos) && entityPlayer.fallDistance > 3) return alert(entityPlayer, CheatType.NoFall)
            }
        }
    }
}