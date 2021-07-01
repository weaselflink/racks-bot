package sc2bots

import com.github.ocraft.s2client.protocol.game.raw.StartRaw
import com.github.ocraft.s2client.protocol.spatial.Point
import java.lang.Float.min
import java.lang.Float.max

class GameMap(
    private val startRaw: StartRaw
) {

    private val width by lazy { startRaw.mapSize.x }
    private val height by lazy { startRaw.mapSize.x }

    fun clampToMap(point: Point) =
        Point.of(
            min(max(0f, point.x), width.toFloat()),
            min(max(0f, point.y), height.toFloat())
        )
}
