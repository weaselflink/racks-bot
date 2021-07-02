package sc2bots

import com.github.ocraft.s2client.bot.S2Agent
import com.github.ocraft.s2client.protocol.spatial.Point
import java.lang.Float.min
import java.lang.Float.max

class GameMap(
    private val sc2Agent: S2Agent
) {

    private val startRaw by lazy {
        sc2Agent.observation().gameInfo.startRaw.get()
    }
    var expansions: List<Point> = emptyList()
        private set

    private val width by lazy { startRaw.mapSize.x }
    private val height by lazy { startRaw.mapSize.x }
    val center: Point by lazy { Point.of(width / 2f, height / 2f) }

    fun initExpansions() {
        expansions = sc2Agent.query().calculateExpansionLocations(sc2Agent.observation())
    }

    fun clampToMap(point: Point) =
        Point.of(
            min(max(0f, point.x), width.toFloat()),
            min(max(0f, point.y), height.toFloat())
        )
}
