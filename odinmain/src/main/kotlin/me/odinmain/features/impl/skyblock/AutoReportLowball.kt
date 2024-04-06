package me.odinmain.features.impl.skyblock

import com.sun.org.apache.xpath.internal.operations.Bool
import me.odinmain.events.impl.ChatPacketEvent
import me.odinmain.events.impl.RealServerTick
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.impl.floor7.p3.GoldorTimer
import me.odinmain.features.settings.impl.BooleanSetting
import me.odinmain.features.settings.impl.HudSetting
import me.odinmain.font.OdinFont
import me.odinmain.ui.hud.HudElement
import me.odinmain.utils.render.Color
import me.odinmain.utils.render.getTextWidth
import me.odinmain.utils.render.text
import me.odinmain.utils.runIn
import me.odinmain.utils.skyblock.modMessage
import me.odinmain.utils.skyblock.reportLowballer
import me.odinmain.utils.skyblock.sendCommand
import net.minecraftforge.fml.common.eventhandler.Cancelable
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object AutoReportLowball : Module(
    name = "Auto Report Lowballers",
    category = Category.SKYBLOCK,
    description = "Automatically reports lowballers.",
) {
    private val hidelowballers: Boolean by BooleanSetting("Hide Lowballers", default = false, description = "Hides lowballing chat messages")
    private val hud: HudElement by HudSetting("Timer Hud", 10f, 10f, 1f, true) {
        if (it) {
            text("§7CD: §a59t", 1f, 9f, Color.RED, 12f, OdinFont.REGULAR, shadow = true)
            getTextWidth("Cooldown: 59t", 12f) + 2f to 16f
        } else {
            if (waitTime.time <= 0) return@HudSetting 0f to 0f
            val displayCD = String.format("%.2f", waitTime.time.toFloat() / 20)

            text("§7CD: §e${displayCD}s", 1f, 9f, Color.WHITE, 12f, OdinFont.REGULAR, shadow = true)
            getTextWidth("Cooldown: 59t", 12f) + 2f to 12f
        }
    }

    private val rateLimitRegex = Regex("Please wait before trying to run this command again!")
    private val bruhRegex = Regex("That player hasn't sent any messages recently!")
    private val confirmRegex = Regex("Please type /report confirm to log your report for staff review.")
    private val chatRegex = Regex("\\[(\\d+)] (.+) (.+): (.+)")
    private val lowballRegex = Regex("(?i)lowball")
    data class Timer(var time: Int)
    private var waitTime = Timer(0)

    @SubscribeEvent
    fun onChat(event: ChatPacketEvent) {
        val message = event.message
        if (message.matches(confirmRegex)) {
            runIn(20) {
                modMessage("§eConfirming report")
                sendCommand("report confirm")
            }
            event.isCanceled = true
        }
        if (message.matches(rateLimitRegex)) {
            modMessage("§cHypixel Hates fun. (Report failed: Wait a bit)")
            waitTime.time = 1200
            event.isCanceled = true
        }

        if (message.matches(bruhRegex)) {
            modMessage("§eHypixel Hates fun. (Report failed: hasn't sent any messages recently)")
            waitTime.time = 0
            event.isCanceled = true
        }

        val ign = chatRegex.matchEntire(message)?.groups?.get(3)?.value ?: return
        val msg = chatRegex.matchEntire(message)?.groups?.get(4)?.value ?: return


        if (msg.contains(lowballRegex)) {
            if (waitTime.time <= 0) {
                waitTime = Timer(400)
                modMessage("§ereporting $ign")
                runIn(6) {
                    reportLowballer(ign)
                }
            }
            if (hidelowballers) { event.isCanceled = true }
        }

    }
    @SubscribeEvent
    fun onServerTick(event: RealServerTick) {
        if (waitTime.time >= 0) {
            waitTime.time--
        }
    }
}