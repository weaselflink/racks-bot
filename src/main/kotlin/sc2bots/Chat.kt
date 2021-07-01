package sc2bots

import com.github.ocraft.s2client.bot.S2Agent
import com.github.ocraft.s2client.protocol.action.ActionChat

class Chat(
    private val sc2Agent: S2Agent
) {

    fun sendChat(message: String) {
        sc2Agent
            .actions()
            .sendChat(message, ActionChat.Channel.BROADCAST)
    }
}
