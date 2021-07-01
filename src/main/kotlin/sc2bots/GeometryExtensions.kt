package sc2bots

import com.github.ocraft.s2client.protocol.spatial.Point

fun Point.towards(to: Point, distance: Float): Point {
    val dir = to.sub(this)
    val dist = to.distance(this).toFloat()
    if (dist < 0.1) {
        return this
    }
    return this.add(dir.div(dist).mul(distance))
}
