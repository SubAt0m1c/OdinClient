package me.odinmain.features.impl.subaddons

import me.odinmain.config.Config
import me.odinmain.events.impl.ChatPacketEvent
import me.odinmain.events.impl.RealServerTick
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.settings.Setting.Companion.withDependency
import me.odinmain.features.settings.impl.*
import me.odinmain.font.OdinFont
import me.odinmain.ui.hud.HudElement
import me.odinmain.utils.render.Color
import me.odinmain.utils.render.getTextWidth
import me.odinmain.utils.render.text
import me.odinmain.utils.runIn
import me.odinmain.utils.skyblock.modMessage
import me.odinmain.utils.skyblock.sendCommand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object AntiLowball : Module(
    name = "Anti-Lowball",
    category = Category.SUBADDONS,
    description = "Automatically reports lowballers.",
) {
    private val isHidden: Boolean by BooleanSetting("Hidden", default = false, description = "Stops showing mod messages and does everything in the background")
    private val hidelowballers: Boolean by BooleanSetting("Hide Lowballers", default = false, description = "Hides lowballing chat messages")
    private val lowballersReported = +NumberSetting("Number of lowballers reported", 0, increment = 0.01, hidden = true)
    private val reportCD: Int by NumberSetting("Report CD", default = 400, min = 0, max = 6000, increment = 20, description = "Cooldown triggered after starting a report, time is in ticks.")
    private val rateLimitCD: Int by NumberSetting("Rate Limit CD", default = 1200, min = 0, max = 12000, increment = 20, description = "Cooldown triggered after getting rate limited, time is in ticks.")
    private val alwaysShowCd: Boolean by BooleanSetting("Always show cd", default = false, description = "Always show cd/people reported")
    private val hideHudText: Boolean by BooleanSetting("hide hud text", default = false, description = "hides the text in the hud")
    private val statusText: Boolean by DropdownSetting("Status Text")
    private val reportingText: String by StringSetting("Reporting Text", "reporting", 128, description = "Message sent before the ign of the player being reported").withDependency { statusText }
    private val confirmingText: String by StringSetting("Confirming Text", "Confirming report...", 128, description = "Message sent when confirming a report").withDependency { statusText }
    private val confirmedText: String by StringSetting("Confirmed Text", "I hate lowballers", 128, description = "Message sent when a report is confirmed").withDependency { statusText }
    private val failureText: String by StringSetting("Failure Text", "Hypixel Hates fun", 128, description = "Message sent when a report has failed").withDependency { statusText }
    private val hud: HudElement by HudSetting("Anti Lowballer hud", 10f, 10f, 1f, true) {
        if (it) {
            text("§7CD: §a59t §7: 1", 1f, 9f, Color.RED, 12f, OdinFont.REGULAR, shadow = true)
            getTextWidth("Cooldown:  59 §7: §r1", 12f) + 2f to 16f
        } else {
            val hudText = if(!hideHudText) { "§7AntiLB: " } else { "" }
            if (!alwaysShowCd && waitTime.time <= 0) return@HudSetting 0f to 0f
            val displayCD = String.format("%.2f", waitTime.time.toFloat() / 20)

            text("$hudText§e${displayCD}s  §7//  §c${lowballersReported.value}", 1f, 9f, Color.WHITE, 12f, OdinFont.REGULAR, shadow = true)
            getTextWidth("Cooldown: 59t : 1", 12f) + 2f to 12f
        }
    }

    private val rateLimitRegex = Regex("Please wait before trying to run this command again!")
    private val bruhRegex = Regex("That player hasn't sent any messages recently!")
    private val confirmRegex = Regex("Please type /report confirm to log your report for staff review.")
    private val confirmedRegex = Regex("Thanks for your Public Chat report. We understand your concerns and it will be reviewed as soon as possible.")
    private val broRegex = Regex("You can't report yourself!")
    private val chatRegex = Regex("\\[(\\d+)] (.+) (.+): (.+)")
    private val lowballRegex = Regex("(?i)lowball")
    data class Timer(var time: Int)
    private var waitTime = Timer(0)

    @SubscribeEvent
    fun onChat(event: ChatPacketEvent) {
        val message = event.message
        if (message.matches(confirmRegex)) {
            runIn(20) {
                if (!isHidden) modMessage("§e$confirmingText")
                sendCommand("report confirm")
            }
            event.isCanceled = true
        }
        if (message.matches(rateLimitRegex)) {
            if (!isHidden) modMessage("§c$failureText (Report failed: Wait a bit)")
            waitTime.time = rateLimitCD
            event.isCanceled = true
        }

        if (message.matches(confirmedRegex)) {
            if (!isHidden) modMessage("§z$confirmedText (Report confirmed!)")
            lowballersReported.value += 1
            Config.save()
            event.isCanceled = true
        }

        if (message.matches(broRegex)) {
            if (!isHidden) modMessage("§c§lSTOP LOWBALLING, YOURE THE PROBLEM")
        }

        if (message.matches(bruhRegex)) {
            if (!isHidden) modMessage("§c$failureText (Report failed: No Recent Messages)")
            waitTime.time = 0
            event.isCanceled = true
        }

        val ign = chatRegex.matchEntire(message)?.groups?.get(3)?.value ?: return
        val msg = chatRegex.matchEntire(message)?.groups?.get(4)?.value ?: return


        if (msg.contains(lowballRegex)) {
            if (waitTime.time <= 0) {
                waitTime.time = reportCD
                if (!isHidden) modMessage("§d$reportingText $ign...")
                runIn(6) {
                    sendCommand("cr $ign")
                }
            }
            if (hidelowballers) { event.isCanceled = true }
        }

    }
    @SubscribeEvent
    fun onServerTick(event: RealServerTick) {
        if (waitTime.time > 0) {
            waitTime.time--
        }
    }
}