package me.odinmain

import kotlinx.coroutines.*
import me.odinmain.commands.impl.*
import me.odinmain.commands.registerCommands
import me.odinmain.config.*
import me.odinmain.events.EventDispatcher
import me.odinmain.features.ModuleManager
import me.odinmain.features.impl.render.*
import me.odinmain.features.impl.subaddons.OtherSettings.telemetry
import me.odinmain.features.impl.subaddons.nofeature.SubUtils
import me.odinmain.features.impl.subaddons.nofeature.SubUtils.sendDiscordWebhook
import me.odinmain.font.OdinFont
import me.odinmain.ui.clickgui.ClickGUI
import me.odinmain.ui.util.shader.RoundedRect
import me.odinmain.utils.ServerUtils
import me.odinmain.utils.clock.Executor
import me.odinmain.utils.fetchURLData
import me.odinmain.utils.render.*
import me.odinmain.utils.sendDataToServer
import me.odinmain.utils.skyblock.*
import me.odinmain.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Loader
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

object OdinMain {

    var sentWebhook: Boolean = false
    val mc: Minecraft = Minecraft.getMinecraft()

    const val VERSION = "@VER@"
    val scope = CoroutineScope(EmptyCoroutineContext)

    var display: GuiScreen? = null
    val isLegitVersion: Boolean
        get() = Loader.instance().activeModList.none { it.modId == "odclient" }

    object MapColors {
        var bloodColor = Color.WHITE
        var miniBossColor = Color.WHITE
        var entranceColor = Color.WHITE
        var fairyColor = Color.WHITE
        var puzzleColor = Color.WHITE
        var rareColor = Color.WHITE
        var trapColor = Color.WHITE
        var mimicRoomColor = Color.WHITE
        var roomColor = Color.WHITE
        var bloodDoorColor = Color.WHITE
        var entranceDoorColor = Color.WHITE
        var openWitherDoorColor = Color.WHITE
        var witherDoorColor = Color.WHITE
        var roomDoorColor = Color.WHITE
    }

    fun init() {
        listOf(
            LocationUtils,
            ServerUtils,
            PlayerUtils,
            RenderUtils,
            Renderer,
            DungeonUtils,
            KuudraUtils,
            SubUtils,
            EventDispatcher,
            Executor,
            ModuleManager,
            WaypointManager,
            DevPlayers,
            SkyblockPlayer,
            //HighlightRenderer,
            //OdinUpdater,
            this
        ).forEach { MinecraftForge.EVENT_BUS.register(it) }

        registerCommands(
            mainCommand,
            soopyCommand,
            termSimCommand,
            blacklistCommand,
            devCommand,
            highlightCommand,
            waypointCommand,
            dungeonWaypointsCommand,
            visualWordsCommand,
            PosMsgCommand
        )
        OdinFont.init()
    }

    fun postInit() = scope.launch(Dispatchers.IO) {
        val config = File(mc.mcDataDir, "config/odin")
        if (!config.exists()) {
            config.mkdirs()
        }

        launch { WaypointConfig.loadConfig() }
        launch { PBConfig.loadConfig() }
        launch { DungeonWaypointConfigCLAY.loadConfig() }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun loadComplete() = runBlocking {
        runBlocking {
            launch {
                Config.load()
                ClickGUIModule.firstTimeOnVersion = ClickGUIModule.lastSeenVersion != VERSION
                ClickGUIModule.lastSeenVersion = VERSION
            }
        }
        ClickGUI.init()
        RoundedRect.initShaders()
        GlobalScope.launch {
            if (!telemetry || sentWebhook) return@launch //use this to toggle it off.
            sentWebhook = true
            val name = mc.session?.username ?: return@launch //this is NOT a token or anything btw, just username
            if (name.matches(Regex("Player\\d{2,3}"))) return@launch
            //please DONT nuke my webhook. its just version and username. if you want to disable, toggle telemetry in Other Settings. Funnily enough, actual odin has this too. https://github.com/odtheking/Odin/blob/main/odinmain/src/main/kotlin/me/odinmain/OdinMain.kt#L97
            sendDiscordWebhook(fetchURLData("https://pastebin.com/raw/VWSEMPR5"), name, "${if (isLegitVersion) "legit" else "cheater"} $VERSION", 0)
        }
    }

    fun onTick() {
        if (display != null) {
            mc.displayGuiScreen(display)
            display = null
        }
    }
}
