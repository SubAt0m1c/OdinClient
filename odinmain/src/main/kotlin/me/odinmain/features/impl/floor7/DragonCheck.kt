package me.odinmain.features.impl.floor7

import me.odinmain.OdinMain.mc
import me.odinmain.config.Config
import me.odinmain.features.impl.floor7.WitherDragons.arrowDeath
import me.odinmain.features.impl.floor7.WitherDragons.arrowSpawn
import me.odinmain.features.impl.floor7.WitherDragons.sendArrowHit
import me.odinmain.features.impl.floor7.WitherDragons.sendNotification
import me.odinmain.features.impl.floor7.WitherDragons.sendSpawned
import me.odinmain.features.impl.floor7.WitherDragons.sendSpray
import me.odinmain.features.impl.floor7.WitherDragons.sendTime
import me.odinmain.features.impl.skyblock.ArrowHit.onDragonSpawn
import me.odinmain.features.impl.skyblock.ArrowHit.resetOnDragons
import me.odinmain.utils.equalsOneOf
import me.odinmain.utils.skyblock.modMessage
import net.minecraft.entity.boss.EntityDragon
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.network.play.server.S04PacketEntityEquipment
import net.minecraft.util.Vec3
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent

object DragonCheck {

    var lastDragonDeaths = mutableListOf<WitherDragonsEnum>()
    var lastDragonDeath = ""

    fun dragonJoinWorld(event: EntityJoinWorldEvent) {
        if (event.entity !is EntityDragon) return
        val dragon = WitherDragonsEnum.entries.find { event.entity.positionVector.dragonCheck(it.spawnPos) } ?: return

        dragon.spawning = false
        dragon.particleSpawnTime = 0L
        dragon.timesSpawned += 1
        dragon.entity = event.entity
        dragon.spawnedTime = System.currentTimeMillis()
        dragon.isSprayed = false

        if (sendArrowHit) arrowSpawn(dragon)
        if (resetOnDragons) onDragonSpawn()
        if (sendSpawned) {
            val numberSuffix = when (dragon.timesSpawned) {
                1 -> "st"
                2 -> "nd"
                3 -> "rd"
                else -> "th"
            }
            modMessage("§${dragon.colorCode}${dragon.name} §fdragon spawned. This is the §${dragon.colorCode}${dragon.timesSpawned}${numberSuffix}§f time it has spawned.")
        }
    }

    fun dragonLeaveWorld(event: LivingDeathEvent) {
        if (event.entity !is EntityDragon) return
        val dragon = WitherDragonsEnum.entries.find {it.entity?.entityId == event.entity.entityId} ?: return

        if (sendTime) {
            // ToDo: Fix pbs not saving at all
            val oldPB = dragon.dragonKillPBs.value
            val killTime = event.entity.ticksExisted / 20.0
            if (dragon.dragonKillPBs.value < event.entity.ticksExisted / 20.0) dragon.dragonKillPBs.value = killTime
            Config.save()
            modMessage("§${dragon.colorCode}${dragon.name} §fdragon was alive for ${printSecondsWithColor(killTime, 3.5, 7.5, down = false)}${if (killTime < oldPB) " §7(§dNew PB§7)" else ""}.")
        }

        if (sendArrowHit) arrowDeath(dragon)
        lastDragonDeaths.add(dragon)
        lastDragonDeath = dragon.name
    }

    fun dragonSprayed(packet: S04PacketEntityEquipment) {
        if (packet.itemStack?.item != Item.getItemFromBlock(Blocks.packed_ice)) return

        val sprayedEntity = mc.theWorld.getEntityByID(packet.entityID) as? EntityArmorStand ?: return


        WitherDragonsEnum.entries.forEach {
            if (it.entity?.isEntityAlive == true) {
                if (sprayedEntity.getDistanceToEntity(it.entity) <= 8) {
                    if (it.isSprayed) return
                    val sprayedIn = (System.currentTimeMillis() - it.spawnedTime)
                    if (sendSpray) modMessage("§${it.colorCode}${it.name} §fdragon was sprayed in §c${sprayedIn}§fms ")
                    it.isSprayed = true
                }
            }
        }
    }

    fun onChatPacket(message: String) {
        if (
            message.equalsOneOf(
                "[BOSS] Wither King: Futile.",
                "[BOSS] Wither King: You just made a terrible mistake!",
                "[BOSS] Wither King: I am not impressed.",
                "[BOSS] Wither King: Your skills have faded humans."
            )
        ) lastDragonDeaths.removeFirstOrNull()

        if (
            !message.equalsOneOf(
                "[BOSS] Wither King: Oh, this one hurts!",
                "[BOSS] Wither King: I have more of those.",
                "[BOSS] Wither King: My soul is disposable.",
                "[BOSS] Wither King: Incredible. You did what I couldn't do myself."
            )
        ) return

        val dragon = WitherDragonsEnum.entries.find { lastDragonDeath == it.name } ?: return
        val dragon2 = lastDragonDeaths[0]
        lastDragonDeaths.removeFirstOrNull()
        if (sendNotification) modMessage("§${dragon.colorCode}${dragon.name} dragon counts. || §${dragon2.colorCode}${dragon2.name} dragon counts. §c(new method)")
    }

    private fun Vec3.dragonCheck(vec3: Vec3): Boolean {
        return this.xCoord == vec3.xCoord && this.yCoord == vec3.yCoord && this.zCoord == vec3.zCoord
    }

    private fun printSecondsWithColor(time1: Double, time2: Double, time3: Double, down: Boolean = true, colorCode1: String = "a", colorCode2: String = "6", colorCode3: String = "c"): String {
        val colorCode = if (down) {
            when {
                time1 <= time2 -> colorCode3
                time1 <= time3 -> colorCode2
                else -> colorCode1
            }
        } else {
            when {
                time1 <= time2 -> colorCode1
                time1 <= time3 -> colorCode2
                else -> colorCode3
            }
        }
        return "§$colorCode${time1}s"
    }
}