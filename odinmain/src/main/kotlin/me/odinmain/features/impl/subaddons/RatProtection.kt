package me.odinmain.features.impl.subaddons

import com.google.gson.JsonObject
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.odinmain.events.impl.ServerTickEvent
import java.util.UUID
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.settings.impl.HudSetting
import me.odinmain.font.OdinFont
import me.odinmain.ui.hud.HudElement
import me.odinmain.utils.render.Color
import me.odinmain.utils.render.getTextWidth
import me.odinmain.utils.render.text
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object RatProtection : Module(
    name = "Rat Protection",
    category = Category.SUBADDONS,
    description = "Protects against getting logged"
){
    private val hud: HudElement by HudSetting("Protection Hud", 10f, 10f, 1f, true) {
        if (it) {
            text("§7Protecting...", 1f, 9f, Color.RED, 12f, OdinFont.REGULAR, shadow = true)
            getTextWidth("Tick: 59t", 12f) + 2f to 16f
        } else {
            val text = if (isProtected) "Protected!" else "Protecting..."
            text("§7$text", 1f, 9f, Color.WHITE, 12f, OdinFont.REGULAR, shadow = true)
            getTextWidth(text, 12f) + 2f to 12f
        }
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent) {
        postToMojang(Minecraft.getMinecraft().session.token, Minecraft.getMinecraft().session.playerID.toString().replace("-",""), UUID.randomUUID().toString().replace("-",""))
    }

    private var isProtected = false

    @OptIn(DelicateCoroutinesApi::class)
    private fun postToMojang(token: String, uuidc: String, svid: String) {
        GlobalScope.launch {
            //This ONLY posts to mojang. If you still think it's a rat, you can disable the module by removing this code.
            val url = URL("https://sessionserver.mojang.com/session/minecraft/join")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            val jsonObject = JsonObject()
            jsonObject.addProperty("accessToken", token)
            jsonObject.addProperty("selectedProfile", uuidc)
            jsonObject.addProperty("serverId", svid)

            val outputStream = connection.outputStream
            outputStream.write(jsonObject.toString().toByteArray())
            outputStream.flush()
            outputStream.close()

            try {
                val responseCode = connection.responseCode
                isProtected = responseCode != 204
            } catch (e: IOException) {
                isProtected = true
            }
        }
    }
}