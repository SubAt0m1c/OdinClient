package me.odinmain.commands.impl

import com.github.stivais.commodore.utils.GreedyString
import me.odinmain.commands.commodore
import me.odinmain.config.Config
import me.odinmain.features.impl.dungeon.PosMessages.posMessageStrings
import me.odinmain.utils.skyblock.modMessage

val PosMsgCommand = commodore("posmsg") {
    literal("add").runs { x: Double, y: Double, z: Double, delay: Long, message: GreedyString ->
        modMessage("Message \"${message}\" added at $x, $y, $z, with ${delay}ms delay")
        val saveData = "x: ${x}, y: ${y}, z: ${z}, delay: ${delay}, message: \"${message}\""
        posMessageStrings.add(saveData)
        Config.save()
    }

    literal("remove").runs { index: Int ->
        if (posMessageStrings.getOrNull(index) == null) return@runs modMessage("Theres no message in position #$index")
        modMessage("Removed Positional Message #$index")
        posMessageStrings.removeAt(index-1)
        Config.save()
    }

    literal("clear").runs {
        modMessage("Cleared List")
        posMessageStrings.clear()
        Config.save()
    }

    literal("list").runs {
        val output = posMessageStrings.joinToString(separator = "\n") {
            "${posMessageStrings.indexOf(it) + 1}: " + it
        }
        modMessage(if(posMessageStrings.isEmpty()) "Positional Message list is empty!" else "Positonal Message list:\n$output")
    }
}