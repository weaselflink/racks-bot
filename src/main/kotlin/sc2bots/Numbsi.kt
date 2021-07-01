package sc2bots

import com.github.ocraft.s2client.bot.S2Agent
import com.github.ocraft.s2client.bot.gateway.UnitInPool
import com.github.ocraft.s2client.protocol.action.ActionChat
import com.github.ocraft.s2client.protocol.data.Abilities
import com.github.ocraft.s2client.protocol.data.UnitType
import com.github.ocraft.s2client.protocol.data.Units
import com.github.ocraft.s2client.protocol.spatial.Point
import com.github.ocraft.s2client.protocol.spatial.Point2d
import com.github.ocraft.s2client.protocol.unit.Alliance
import com.github.ocraft.s2client.protocol.unit.Unit
import kotlin.random.Random

class Numbsi : S2Agent() {

    private val gameMap by lazy { GameMap(observation().gameInfo.startRaw.get()) }

    private val buildingAbilities = mapOf(
        Units.TERRAN_COMMAND_CENTER to Abilities.BUILD_COMMAND_CENTER,
        Units.TERRAN_REFINERY to Abilities.BUILD_REFINERY,
        Units.TERRAN_SUPPLY_DEPOT to Abilities.BUILD_SUPPLY_DEPOT,
        Units.TERRAN_BARRACKS to Abilities.BUILD_BARRACKS
    )

    private val trainingAbilities = mapOf(
        Units.TERRAN_SCV to Abilities.TRAIN_SCV,
        Units.TERRAN_MARINE to Abilities.TRAIN_MARINE
    )

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

    private val unitTypes = setOf(
        Units.TERRAN_SCV,
        Units.TERRAN_MARINE,
        Units.TERRAN_MARAUDER
    )

    private val townHallTypes = setOf(
        Units.TERRAN_COMMAND_CENTER,
        Units.TERRAN_PLANETARY_FORTRESS,
        Units.TERRAN_ORBITAL_COMMAND
    )

    override fun onGameStart() {
        sendChat("GLHF")
    }

    override fun onStep() {
        if (supplyLeft < 4 && !isPending(Units.TERRAN_SUPPLY_DEPOT)) {
            tryBuildStructure(Units.TERRAN_SUPPLY_DEPOT)
        }
        if (ownStructures.ofType(Units.TERRAN_BARRACKS).isEmpty() && !isPending(Units.TERRAN_BARRACKS)) {
            tryBuildStructure(Units.TERRAN_BARRACKS)
        }
        tryTrainScv()
        tryTrainMarine()
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

    private fun tryTrainScv() {
        if (supplyLeft >= 1 && canAfford(Units.TERRAN_SCV)) {
            townHalls
                .asUnits()
                .idle()
                .randomOrNull()
                ?.train(Units.TERRAN_SCV)
        }
    }

    private fun tryTrainMarine() {
        if (supplyLeft >= 1 && canAfford(Units.TERRAN_MARINE)) {
            ownStructures
                .ofType(Units.TERRAN_BARRACKS)
                .randomOrNull()
                ?.train(Units.TERRAN_MARINE)
        }
    }

    private fun Unit.train(units: Units) {
        val ability = trainingAbilities[units] ?: return
        actions().unitCommand(this, ability, false)
    }

    private fun tryBuildStructure(building: Units) {
        if (!canAfford(building)) {
            return
        }
        val ability = buildingAbilities[building] ?: return
        val builder = workers.randomOrNull() ?: return
        val cc = townHalls
            .first()
            .unit()
            .position
        val spot = cc
            .towards(mapCenter, 8f)
            .add(Point.of(getRandomScalar(), getRandomScalar()).mul(5.0f))
        val clamped = gameMap.clampToMap(spot)
        actions()
            .unitCommand(
                builder,
                ability,
                clamped.toPoint2d(),
                false
            )
    }

    private fun isPending(building: Units): Boolean {
        val partial = ownStructures.ofType(building)
            .any { it.buildProgress < 1.0 }
        if (partial) {
            return true
        }
        val ability = buildingAbilities[building] ?: return false
        return workers
            .any { worker ->
                worker.orders
                    .any { it.ability == ability }
            }
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

    private val mapCenter by lazy {
        observation().gameInfo
            .findCenterOfMap()
            .let { Point.of(it.x.toFloat(), it.y.toFloat()) }
    }

    private val ownUnits
        get() = observation()
            .getUnits { it.unit().alliance == Alliance.SELF }
            .filter { it.unit().type in unitTypes }

    private val ownStructures
        get() = observation()
            .getUnits { it.unit().alliance == Alliance.SELF }
            .filter { it.unit().type in structureTypes }

    private val supplyLeft
        get() = observation().foodCap - observation().foodUsed

    private val workers
        get() = ownUnits.ofType(Units.TERRAN_SCV)

    private val townHalls: List<UnitInPool>
        get() = ownStructures.filter { it.unit().type in townHallTypes }

    private fun Iterable<UnitInPool>.ofType(vararg unitType: UnitType) =
        asUnits()
            .filter { it.type in unitType }

    private fun Iterable<UnitInPool>.asUnits() = map { it.unit() }

    private fun Iterable<Unit>.ready() = filter { it.buildProgress >= 1.0 }

    private fun Iterable<Unit>.idle() = ready().filter { it.orders.isEmpty() }

    private fun sendChat(message: String) =
        actions().sendChat(message, ActionChat.Channel.BROADCAST)

    private fun canAfford(unitType: UnitType) = canAfford(cost(unitType))

    private fun canAfford(cost: Cost?): Boolean {
        return cost != null &&
            cost.minerals <= observation().minerals &&
            cost.vespene <= observation().vespene
    }

    private fun cost(unitType: UnitType) =
        observation().getUnitTypeData(false)[unitType]
            ?.let {
                Cost(it.mineralCost.orElse(0), it.vespeneCost.orElse(0))
            }
}

data class Cost(
    val minerals: Int,
    val vespene: Int
)
