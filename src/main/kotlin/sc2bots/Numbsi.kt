package sc2bots

import com.github.ocraft.s2client.bot.gateway.UnitInPool
import com.github.ocraft.s2client.protocol.data.Abilities
import com.github.ocraft.s2client.protocol.data.UnitType
import com.github.ocraft.s2client.protocol.data.Units
import com.github.ocraft.s2client.protocol.debug.Color
import com.github.ocraft.s2client.protocol.spatial.Point
import com.github.ocraft.s2client.protocol.spatial.Point2d
import com.github.ocraft.s2client.protocol.unit.Alliance
import com.github.ocraft.s2client.protocol.unit.Unit
import kotlin.random.Random

class Numbsi : TerranBot() {

    override fun onStep() {
        debug().debugSphereOut(townHalls.first().position, 9f, Color.WHITE)
        if (supplyLeft < 4 && !isPending(Units.TERRAN_SUPPLY_DEPOT)) {
            tryBuildStructure(Units.TERRAN_SUPPLY_DEPOT)
        }
        if (ownStructures.ofType(Units.TERRAN_BARRACKS).isEmpty() && !isPending(Units.TERRAN_BARRACKS)) {
            tryBuildStructure(Units.TERRAN_BARRACKS)
        }
        tryTrainScv()
        tryTrainMarine()

        debug().sendDebug()
    }

    override fun onUnitIdle(unitInPool: UnitInPool) {
        val unit = unitInPool.unit()
        when (unit.type) {
            Units.TERRAN_SCV -> {
                val pos = unit.position.toPoint2d()
                findNearestUnit(pos, Units.TERRAN_COMMAND_CENTER)
                    ?.let {
                        findNearestMineralPatch(it.position.toPoint2d())
                    }
                    ?.also {
                        actions().unitCommand(unit, Abilities.HARVEST_GATHER_SCV, it, true)
                    }
            }
        }
    }

    override fun onBuildingConstructionComplete(unitInPool: UnitInPool) {
        val unit = unitInPool.unit()
        when (unit.type) {
            Units.TERRAN_SUPPLY_DEPOT -> {
                actions()
                    .unitCommand(unit, Abilities.MORPH_SUPPLY_DEPOT_LOWER, false)
            }
        }
    }

    private fun tryBuildStructure(building: Units) {
        val cc = townHalls
            .first()
            .position
        val spot = cc
            .towards(gameMap.center, 8f)
            .add(Point.of(getRandomScalar(), getRandomScalar()).mul(5.0f))
        val clamped = gameMap.clampToMap(spot)
        tryBuildStructure(building, clamped)
    }

    private fun getRandomScalar(): Float {
        return Random.nextFloat() * 2 - 1
    }

    private fun findNearestUnit(
        from: Point2d,
        unitType: UnitType,
        alliance: Alliance = Alliance.SELF
    ): Unit? {
        return observation()
            .getUnits(alliance)
            .map { it.unit() }
            .filter { it.type == unitType }
            .minByOrNull { it.position.toPoint2d().distance(from) }
    }

    private fun findNearestMineralPatch(from: Point2d): Unit? {
        return findNearestUnit(
            from,
            Units.NEUTRAL_MINERAL_FIELD,
            Alliance.NEUTRAL
        )
    }
}
