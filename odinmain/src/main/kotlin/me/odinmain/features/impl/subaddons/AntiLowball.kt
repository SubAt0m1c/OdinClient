package me.odinmain.features.impl.subaddons

import me.odinmain.config.Config
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

object AntiLowball : Module(
    name = "Anti-Lowball",
    category = Category.SUBADDONS,
    description = "Automatically reports lowballers.",
) {
    private val isHidden: Boolean by BooleanSetting("Hidden", default = false, description = "Stops showing mod messages and does everything in the background")
    private val hidelowballers: Boolean by BooleanSetting("Hide Lowballers", default = false, description = "Hides lowballing chat messages")
    private val lowballersReported = +NumberSetting("Number of lowballers reported", 0, increment = 0.01, hidden = true)
    private val reportCD: Int by NumberSetting("Report CD", default = 10, min = 0, max = 120, increment = 1, description = "Cooldown triggered after starting a report, time is in Seconds.")
    private val rateLimitCD: Int by NumberSetting("Rate Limit CD", default = 30, min = 0, max = 240, increment = 1, description = "Cooldown triggered after getting rate limited, time is in Seconds.")
    private val alwaysShowCd: Boolean by BooleanSetting("Always show cd", default = false, description = "Always show cd/people reported")
    private val hideHudText: Boolean by BooleanSetting("hide hud text", default = false, description = "hides the text in the hud")
    private val informAdd: Boolean by BooleanSetting("Notify Que Add", default = true, description = "Notifies you when a player has been added to the que.")
    private val statusText: Boolean by DropdownSetting("Status Text")
    private val reportingText: String by StringSetting("Reporting Text", "reporting", 128, description = "Message sent before the ign of the player being reported").withDependency { statusText }
    private val confirmingText: String by StringSetting("Confirming Text", "Confirming report...", 128, description = "Message sent when confirming a report").withDependency { statusText }
    private val confirmedText: String by StringSetting("Confirmed Text", "I hate lowballers", 128, description = "Message sent when a report is confirmed").withDependency { statusText }
    private val failureText: String by StringSetting("Failure Text", "Hypixel Hates fun", 128, description = "Message sent when a report has failed").withDependency { statusText }
    private val hud: HudElement by HudSetting("Anti Lowballer hud", 10f, 10f, 1f, true) {
        if (it) {
            text("§7AntiLB: §a59t  §7//  §c1 // §68", 1f, 9f, Color.RED, 12f, OdinFont.REGULAR, shadow = true)
            getTextWidth("AntiLB: 59  //  1 //  8", 12f) + 2f to 16f
        } else {
            val hudText = if(!hideHudText) { "§7AntiLB: " } else { "" }
            if (!alwaysShowCd && waitTime.time <= 0) return@HudSetting 0f to 0f
            val displayCD = String.format("%.2f", waitTime.time.toFloat() / 20)

            text("$hudText§e${displayCD}s  §7//  §c${lowballersReported.value} §7//  §6${reportQueue.size}", 1f, 9f, Color.WHITE, 12f, OdinFont.REGULAR, shadow = true)
            getTextWidth("AntiLB: 59  //  1 //  8", 12f) + 2f to 12f
        }
    }

    private val rateLimitRegex = Regex("Please wait before trying to run this command again!")
    private val bruhRegex = Regex("That player hasn't sent any messages recently!")
    private val confirmRegex = Regex("Please type /report confirm to log your report for staff review.")
    private val confirmedRegex = Regex("Thanks for your Public Chat report. We understand your concerns and it will be reviewed as soon as possible.")
    private val chatRegex = Regex("\\[(\\d+)] (.+) (.+): (.+)")
    private val lowballRegex = Regex("(?i)lowball")
    data class Timer(var time: Int)
    private var waitTime = Timer(0)

    private var reportQueue = mutableSetOf<String>()
    
    init {
        execute(50) {
            waitTime.time--
            if (reportQueue.size >= 1 && waitTime.time <= 0) {
                waitTime.time = reportCD * 20
                val ign = reportQueue.firstOrNull()
                if (!isHidden) modMessage("§d$reportingText $ign...")
                runIn(6) {
                    sendCommand("cr $ign")
                }
            }
        }

        onMessageCancellable(Regex("(?s).*")) {
            if (it.message.matches(confirmRegex)) {
                runIn(20) {
                    if (!isHidden) modMessage("§e$confirmingText")
                    sendCommand("report confirm")
                }
                it.isCanceled = true
            }
            if (it.message.matches(rateLimitRegex)) {
                if (!isHidden) modMessage("§c$failureText (Report failed: Wait a bit)")
                waitTime.time = rateLimitCD * 20
                it.isCanceled = true
            }

            if (it.message.matches(confirmedRegex)) {
                if (!isHidden) modMessage("§z$confirmedText (Report on ${reportQueue.firstOrNull()} confirmed!)")
                reportQueue.remove(reportQueue.firstOrNull())
                lowballersReported.value += 1
                Config.save()
                it.isCanceled = true
            }

            if (it.message.matches(bruhRegex)) {
                if (!isHidden) modMessage("§c$failureText (Report failed: No Recent Messages)")
                waitTime.time = 0
                reportQueue.remove(reportQueue.firstOrNull())
                it.isCanceled = true
            }

            val ign = chatRegex.matchEntire(it.message)?.groups?.get(3)?.value ?: return@onMessageCancellable
            val msg = chatRegex.matchEntire(it.message)?.groups?.get(4)?.value ?: return@onMessageCancellable

            if (msg.contains(lowballRegex)) {
                if (hidelowballers) { it.isCanceled = true }
                if (ign == mc.thePlayer.name) return@onMessageCancellable modMessage("§c§lSTOP LOWBALLING, YOURE THE PROBLEM")
                if (reportQueue.contains(ign) && informAdd) return@onMessageCancellable modMessage("§c$ign is already on the que...??? (Where Mute????!!!???)")
                if (informAdd) modMessage("§6Added $ign to the que! there are ${reportQueue.size +1} players before them.")
                reportQueue.add(ign)
            }
        }
    }
}