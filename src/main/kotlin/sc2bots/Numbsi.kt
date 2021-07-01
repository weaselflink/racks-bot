package sc2bots

import com.github.ocraft.s2client.bot.S2Agent
import com.github.ocraft.s2client.bot.gateway.UnitInPool
import com.github.ocraft.s2client.protocol.action.ActionChat
import com.github.ocraft.s2client.protocol.data.Abilities
import com.github.ocraft.s2client.protocol.data.Ability
import com.github.ocraft.s2client.protocol.data.UnitType
import com.github.ocraft.s2client.protocol.data.Units
import com.github.ocraft.s2client.protocol.spatial.Point2d
import com.github.ocraft.s2client.protocol.unit.Alliance
import com.github.ocraft.s2client.protocol.unit.Unit
import kotlin.random.Random


class Numbsi : S2Agent() {

    private val structureTypes = setOf(
        Units.TERRAN_COMMAND_CENTER,
        Units.TERRAN_COMMAND_CENTER_FLYING,
        Units.TERRAN_PLANETARY_FORTRESS,
        Units.TERRAN_ORBITAL_COMMAND,
        Units.TERRAN_ORBITAL_COMMAND_FLYING,
        Units.TERRAN_REFINERY,
        Units.TERRAN_REFINERY_RICH,
        Units.TERRAN_SUPPLY_DEPOT,
        Units.TERRAN_SUPPLY_DEPOT_LOWERED,
        Units.TERRAN_BARRACKS,
        Units.TERRAN_BARRACKS_TECHLAB,
        Units.TERRAN_BARRACKS_REACTOR,
        Units.TERRAN_BARRACKS_FLYING
    )

    override fun onGameStart() {
        actions().sendChat("GLHF", ActionChat.Channel.BROADCAST)
    }

    override fun onStep() {
        tryBuildSupplyDepot()
        tryTrainScv()
    }

    override fun onUnitIdle(unitInPool: UnitInPool) {
        val unit = unitInPool.unit()
        when (unit.type) {
            Units.TERRAN_SCV -> {
                actions()
                    .sendChat("SCV idle", ActionChat.Channel.BROADCAST)
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

    private fun tryTrainScv() {
        if (supplyLeft > 0) {
            townHalls
                .firstOrNull { it.orders.isEmpty() }
                ?.trainScv()
        }
    }

    private fun Unit.trainScv() {
        actions()
            .sendChat("train SCV", ActionChat.Channel.BROADCAST)
        actions()
            .unitCommand(this, Abilities.TRAIN_SCV, false)
    }

    private fun tryBuildSupplyDepot() {
        if (supplyLeft < 2 && observation().minerals >= 100) {
            tryBuildStructure(Abilities.BUILD_SUPPLY_DEPOT, Units.TERRAN_SCV)
        }
    }

    private fun tryBuildStructure(abilityTypeForStructure: Ability, unitType: UnitType) {
        if (observation().getUnits(doesBuildWith(abilityTypeForStructure)).isNotEmpty()) {
            return
        }

        val unitInPool = getRandomUnit(unitType)
        if (unitInPool != null) {
            val unit: Unit = unitInPool.unit()
            val spot = unit.position.toPoint2d().add(Point2d.of(getRandomScalar(), getRandomScalar()).mul(15.0f))
            actions()
                .unitCommand(
                    unit,
                    abilityTypeForStructure,
                    spot,
                    false
                )
        }
    }

    private fun doesBuildWith(abilityTypeForStructure: Ability): (UnitInPool) -> Boolean =
        {
            it.unit()
                .orders
                .any { unitOrder -> abilityTypeForStructure == unitOrder.ability }
        }

    private fun getRandomUnit(unitType: UnitType): UnitInPool? {
        val units = observation().getUnits(Alliance.SELF, UnitInPool.isUnit(unitType))
        return if (units.isEmpty()) null else units.random()
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

    private val supplyLeft
        get() = observation().foodCap - observation().foodUsed

    private val units
        get() = observation().getUnits { it.unit().type !in structureTypes }

    private val structures
        get() = observation().getUnits { it.unit().type in structureTypes }

    private val townHalls
        get() = structures
            .ofType(
                Units.TERRAN_COMMAND_CENTER,
                Units.TERRAN_PLANETARY_FORTRESS,
                Units.TERRAN_ORBITAL_COMMAND
            )

    private fun List<UnitInPool>.ofType(vararg unitType: UnitType) =
        map { it.unit() }
            .filter { it.type in unitType }

    private fun List<UnitInPool>.ready() =
        map { it.unit() }
            .filter { it.buildProgress == 1.0f }
}
