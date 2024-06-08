package me.odinmain.features.impl.subaddons.nofeature

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBlock
import kotlin.math.abs
import kotlin.math.max


class PlayerData {
    var speed: Double = 0.0
    var aboveVoidTicks: Int = 0
    var fastTick: Int = 0
    var autoBlockTicks: Int = 0
    var ticksExisted: Int = 0
    var lastSneakTick: Int = 0
    var posZ: Double = 0.0
    var sneakTicks: Int = 0
    var noSlowTicks: Int = 0
    var posY: Double = 0.0
    var sneaking: Boolean = false
    var posX: Double = 0.0
    var serverPosX: Double = 0.0
    var serverPosY: Double = 0.0
    var serverPosZ: Double = 0.0

    fun update(entityPlayer: EntityPlayer) {
        val ticksExisted = entityPlayer.ticksExisted
        this.posX = entityPlayer.posX - entityPlayer.lastTickPosX
        this.posY = entityPlayer.posY - entityPlayer.lastTickPosY
        this.posZ = entityPlayer.posZ - entityPlayer.lastTickPosZ
        this.speed = max(abs(this.posX), abs(this.posZ))
        if (this.speed >= 0.07) {
            ++this.fastTick
            this.ticksExisted = ticksExisted
        } else {
            this.fastTick = 0
        }
        if (abs(this.posY) >= 0.1) {
            this.aboveVoidTicks = ticksExisted
        }
        if (entityPlayer.isSneaking) {
            this.lastSneakTick = ticksExisted
        }
        if (entityPlayer.isSwingInProgress && entityPlayer.isBlocking) {
            ++this.autoBlockTicks
        } else {
            this.autoBlockTicks = 0
        }
        if (entityPlayer.isSprinting && entityPlayer.isUsingItem) {
            ++this.noSlowTicks
        } else {
            this.noSlowTicks = 0
        }
        if (entityPlayer.rotationPitch >= 70.0f && entityPlayer.heldItem != null && entityPlayer.heldItem.item is ItemBlock) {
            if (entityPlayer.swingProgressInt == 1) {
                if (!this.sneaking && entityPlayer.isSneaking) {
                    ++this.sneakTicks
                } else {
                    this.sneakTicks = 0
                }
            }
        } else {
            this.sneakTicks = 0
        }
    }

    fun updateSneak(entityPlayer: EntityPlayer) {
        this.sneaking = entityPlayer.isSneaking
    }

    fun updateServerPos(entityPlayer: EntityPlayer) {
        this.serverPosX = (entityPlayer.serverPosX / 32).toDouble()
        this.serverPosY = (entityPlayer.serverPosY / 32).toDouble()
        this.serverPosZ = (entityPlayer.serverPosZ / 32).toDouble()
    }
}