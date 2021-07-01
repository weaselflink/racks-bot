package sc2bots

import com.github.ocraft.s2client.bot.S2Coordinator
import com.github.ocraft.s2client.protocol.game.BattlenetMap
import com.github.ocraft.s2client.protocol.game.Difficulty
import com.github.ocraft.s2client.protocol.game.LocalMap
import com.github.ocraft.s2client.protocol.game.Race
import kotlin.io.path.Path

fun main(args: Array<String>) {
    val bot = Numbsi()
    val s2Coordinator = S2Coordinator.setup()
        .setRealtime(false)
        .loadSettings(args)
        .setParticipants(
            S2Coordinator.createParticipant(Race.TERRAN, bot),
            S2Coordinator.createComputer(Race.ZERG, Difficulty.EASY)
        )
        .launchStarcraft()
        .startGame(LocalMap.of(Path("EphemeronLE.SC2Map")))

    while (s2Coordinator.update()) {
        // nothing
    }

    s2Coordinator.quit()
}
