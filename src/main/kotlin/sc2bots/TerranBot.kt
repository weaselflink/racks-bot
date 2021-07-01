package sc2bots

import com.github.ocraft.s2client.bot.S2Agent
import com.github.ocraft.s2client.bot.gateway.UnitInPool
import com.github.ocraft.s2client.protocol.data.Abilities
import com.github.ocraft.s2client.protocol.data.Ability
import com.github.ocraft.s2client.protocol.data.UnitType
import com.github.ocraft.s2client.protocol.data.Units
import com.github.ocraft.s2client.protocol.spatial.Point
import com.github.ocraft.s2client.protocol.unit.Alliance
import com.github.ocraft.s2client.protocol.unit.Unit

open class TerranBot : S2Agent() {

    val chat by lazy { Chat(this) }
    val gameMap by lazy { GameMap(this) }

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

    private val unitTypes = listOf(
        Units.TERRAN_SCV,
        Units.TERRAN_MARINE,
        Units.TERRAN_MARAUDER
    )

    private val townHallTypes = listOf(
        Units.TERRAN_COMMAND_CENTER,
        Units.TERRAN_PLANETARY_FORTRESS,
        Units.TERRAN_ORBITAL_COMMAND
    )

    private val buildingAbilities = mapOf(
        Units.TERRAN_COMMAND_CENTER to Abilities.BUILD_COMMAND_CENTER,
        Units.TERRAN_REFINERY to Abilities.BUILD_REFINERY,
        Units.TERRAN_SUPPLY_DEPOT to Abilities.BUILD_SUPPLY_DEPOT,
        Units.TERRAN_BARRACKS to Abilities.BUILD_BARRACKS
    )

    private val trainings = listOf(
        TrainingData(
            unitType = Units.TERRAN_SCV,
            ability = Abilities.TRAIN_SCV,
            buildingTypes = townHallTypes
        ),
        TrainingData(
            unitType = Units.TERRAN_MARINE,
            ability = Abilities.TRAIN_MARINE,
            buildingTypes = listOf(
                Units.TERRAN_BARRACKS,
                Units.TERRAN_BARRACKS_REACTOR,
                Units.TERRAN_BARRACKS_TECHLAB,
            )
        )
    ).associateBy { it.unitType }

    val ownUnits
        get() = observation()
            .getUnits { it.unit().alliance == Alliance.SELF }
            .asUnits()
            .filter { it.type in unitTypes }

    val ownStructures
        get() = observation()
            .getUnits { it.unit().alliance == Alliance.SELF }
            .asUnits()
            .filter { it.type in structureTypes }

    val supplyLeft
        get() = observation().foodCap - observation().foodUsed

    val workers
        get() = ownUnits.ofType(Units.TERRAN_SCV)

    val townHalls: List<Unit>
        get() = ownStructures.filter { it.type in townHallTypes }

    fun cost(unitType: UnitType) =
        observation().getUnitTypeData(false)[unitType]
            ?.let {
                Cost(
                    supply = it.foodRequired.orElse(0f),
                    minerals = it.mineralCost.orElse(0),
                    vespene = it.vespeneCost.orElse(0)
                )
            }

    fun canTrain(unitType: UnitType): Boolean {
        if (!canAfford(unitType)) {
            return false
        }
        return trainings[unitType]
            ?.let { training ->
                return ownStructures
                    .ready()
                    .filter { it.type in training.buildingTypes }
                    .filter { it.canCast(training.ability) }
                    .any()
            }
            ?: false
    }

    fun Unit.canCast(ability: Ability) =
        query()
            .getAbilitiesForUnit(this, true)
            .abilities
            .map { it.ability }
            .contains(ability)

    fun canAfford(unitType: UnitType) = canAfford(cost(unitType))

    fun canAfford(cost: Cost?): Boolean {
        return cost != null &&
                cost.supply <= supplyLeft &&
                cost.minerals <= observation().minerals &&
                cost.vespene <= observation().vespene
    }

    fun Iterable<Unit>.ofType(vararg unitType: UnitType) =
        filter { it.type in unitType }

    fun Iterable<UnitInPool>.asUnits() = map { it.unit() }

    fun Iterable<Unit>.ready() = filter { it.buildProgress >= 1.0 }

    fun tryTrainScv() = tryTrain(Units.TERRAN_SCV)

    fun tryTrainMarine() = tryTrain(Units.TERRAN_MARINE)

    fun tryTrain(unitType: Units) {
        if (canTrain(unitType)) {
            train(unitType)
        }
    }

    fun train(unitType: Units) {
        val training = trainings[unitType] ?: return
        val building = ownStructures
            .ready()
            .filter { it.type in training.buildingTypes }
            .filter { it.canCast(training.ability) }
            .randomOrNull()
            ?: return
        actions().unitCommand(building, training.ability, false)
    }

    fun isPending(building: Units): Boolean {
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

    fun tryBuildStructure(building: Units, position: Point) {
        if (!canAfford(building)) {
            return
        }
        val ability = buildingAbilities[building] ?: return
        val builder = workers.randomOrNull() ?: return
        actions()
            .unitCommand(
                builder,
                ability,
                position.toPoint2d(),
                false
            )
    }
}

data class Cost(
    val supply: Float,
    val minerals: Int,
    val vespene: Int
)

data class TrainingData(
    val unitType: Units,
    val ability: Ability,
    val buildingTypes: List<Units>
)
