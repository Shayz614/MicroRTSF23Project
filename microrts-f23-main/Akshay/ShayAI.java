<<<<<<< HEAD
package mins_bot;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.ParameterSpecification;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.*;

public class ShayAI extends AbstractionLayerAI {

    private Random randomGenerator = new Random();
    private UnitTypeTable unitTypeTable;
    UnitType workerUnit;
    UnitType baseUnit;
    UnitType barracksUnit;
    UnitType lightInfantry;
    UnitType heavyInfantry;


    public ShayAI(UnitTypeTable utt) {
        this(utt, new AStarPathFinding());
    }

    public ShayAI(UnitTypeTable utt, PathFinding pathFinder) {
        super(pathFinder);
        initializeTypes(utt);
    }

    private void initializeTypes(UnitTypeTable utt) {
        this.unitTypeTable = utt;
        workerUnit = unitTypeTable.getUnitType("Worker");
        baseUnit = unitTypeTable.getUnitType("Base");
        barracksUnit = unitTypeTable.getUnitType("Barracks");
        lightInfantry = unitTypeTable.getUnitType("Light");
        heavyInfantry = unitTypeTable.getUnitType("Heavy");
    }

    @Override
    public void reset() {
        super.reset();
    }

    public void reset(UnitTypeTable utt) {
        initializeTypes(utt);
    }

    @Override
    public AI clone() {
        return new ShayAI(unitTypeTable, pf);
    }
    
public PlayerAction getAction(int player, GameState gameState) {
    PhysicalGameState physicalState = gameState.getPhysicalGameState();
    Player currentPlayer = gameState.getPlayer(player);

    // Managing base actions
    for (Unit unit : physicalState.getUnits()) {
        if (unit.getType() == baseUnit && unit.getPlayer() == player && gameState.getActionAssignment(unit) == null) {
            handleBaseActions(unit, currentPlayer, physicalState);
        }
    }

    // Managing barracks actions
    for (Unit unit : physicalState.getUnits()) {
        if (unit.getType() == barracksUnit && unit.getPlayer() == player && gameState.getActionAssignment(unit) == null) {
            handleBarracksActions(unit, currentPlayer, physicalState);
        }
    }

    // Managing actions of attack units
    for (Unit unit : physicalState.getUnits()) {
        if (unit.getType().canAttack && !unit.getType().canHarvest && unit.getPlayer() == player && gameState.getActionAssignment(unit) == null) {
            handleAttackUnitActions(unit, currentPlayer, gameState);
        }
    }

    // Managing worker units
    List<Unit> workerUnits = new LinkedList<>();
    for (Unit unit : physicalState.getUnits()) {
        if (unit.getType().canHarvest && unit.getPlayer() == player) {
            workerUnits.add(unit);
        }
    }
    handleWorkerActions(workerUnits, currentPlayer, gameState);

    // Consolidating actions into a PlayerAction
    return translateActions(player, gameState);
}

private void handleBaseActions(Unit base, Player player, PhysicalGameState pgs) {
    int workerCount = 0;
    for (Unit unit : pgs.getUnits()) {
        if (unit.getType() == workerUnit && unit.getPlayer() == player.getID()) {
            workerCount++;
        }
    }
    if (workerCount < 3 && player.getResources() >= workerUnit.cost) {
        train(base, workerUnit);
    }
}

private void handleBarracksActions(Unit barracks, Player player, PhysicalGameState pgs) {
    // Count existing light and heavy infantry units
    int lightInfantryCount = 0;
    int heavyInfantryCount = 0;
    for (Unit u : pgs.getUnits()) {
        if (u.getType() == lightInfantry && u.getPlayer() == player.getID()) {
            lightInfantryCount++;
        }
        if (u.getType() == heavyInfantry && u.getPlayer() == player.getID()) {
            heavyInfantryCount++;
        }
    }

    // Decide whether to train light or heavy infantry based on a simple strategy
    boolean shouldTrainLightInfantry = lightInfantryCount <= heavyInfantryCount;

    // Check resources and train units accordingly
    if (shouldTrainLightInfantry && player.getResources() >= lightInfantry.cost) {
        train(barracks, lightInfantry);
    } else if (player.getResources() >= heavyInfantry.cost) {
        train(barracks, heavyInfantry);
    }
}

private void handleAttackUnitActions(GameState gs, PhysicalGameState pgs, Player player) {
    List<Unit> lightInfantryUnits = new ArrayList<>();
    List<Unit> heavyInfantryUnits = new ArrayList<>();

    // Separate light and heavy infantry units
    for (Unit u : pgs.getUnits()) {
        if (u.getPlayer() == player.getID()) {
            if (u.getType() == lightInfantry) {
                lightInfantryUnits.add(u);
            } else if (u.getType() == heavyInfantry) {
                heavyInfantryUnits.add(u);
            }
        }
    }

    // Form groups and assign targets
    while (lightInfantryUnits.size() >= 2 && heavyInfantryUnits.size() >= 1) {
        List<Unit> group = new ArrayList<>();
        group.add(lightInfantryUnits.remove(0));
        group.add(lightInfantryUnits.remove(0));
        group.add(heavyInfantryUnits.remove(0));

        Unit target = findNearestEnemy(group, pgs, player);
        if (target != null) {
            for (Unit unit : group) {
                attack(unit, target);
            }
        }
    }

    //ranged unit if needed.
}

private Unit findNearestEnemy(List<Unit> group, PhysicalGameState pgs, Player player) {
    Unit nearestEnemy = null;
    int minDistance = Integer.MAX_VALUE;
    for (Unit u : pgs.getUnits()) {
        if (u.getPlayer() >= 0 && u.getPlayer() != player.getID()) {
            for (Unit groupUnit : group) {
                int distance = Math.abs(u.getX() - groupUnit.getX()) + Math.abs(u.getY() - groupUnit.getY());
                if (nearestEnemy == null || distance < minDistance) {
                    nearestEnemy = u;
                    minDistance = distance;
                }
            }
        }
    }
    return nearestEnemy;
}
private void handleWorkerActions(List<Unit> workers, Player player, GameState gs) {
    PhysicalGameState pgs = gs.getPhysicalGameState();
    int baseCount = 0;
    int barracksCount = 0;

    int usedResources = 0;
    List<Unit> availableWorkers = new LinkedList<>(workers);

    if (workers.isEmpty()) {
        return;
    }

    // Counting existing bases and barracks
    for (Unit u : pgs.getUnits()) {
        if (u.getType() == baseUnit && u.getPlayer() == player.getID()) {
            baseCount++;
        }
        if (u.getType() == barracksUnit && u.getPlayer() == player.getID()) {
            barracksCount++;
        }
    }

    List<Integer> reservedLocations = new LinkedList<>();
    if (baseCount == 0 && !availableWorkers.isEmpty()) {
        // Building a base
        if (player.getResources() >= baseUnit.cost + usedResources) {
            Unit worker = availableWorkers.remove(0);
            constructBuilding(worker, baseUnit, worker.getX(), worker.getY(), reservedLocations, player, pgs);
            usedResources += baseUnit.cost;
        }
    }

    if (barracksCount == 0) {
        // Building a barracks
        if (player.getResources() >= barracksUnit.cost + usedResources && !availableWorkers.isEmpty()) {
            Unit worker = availableWorkers.remove(0);
            constructBuilding(worker, barracksUnit, worker.getX(), worker.getY(), reservedLocations, player, pgs);
            usedResources += barracksUnit.cost;
        }
    }

    // Assigning remaining workers to harvest resources
    for (Unit worker : availableWorkers) {
        manageResourceHarvesting(worker, pgs, player);
    }
}

private void manageResourceHarvesting(Unit worker, PhysicalGameState pgs, Player player) {
    Unit closestBase = null;
    Unit closestResource = null;
    int closestDistance = Integer.MAX_VALUE;
    for (Unit u2 : pgs.getUnits()) {
        if (u2.getType().isResource) {
            int distance = Math.abs(u2.getX() - worker.getX()) + Math.abs(u2.getY() - worker.getY());
            if (closestResource == null || distance < closestDistance) {
                closestResource = u2;
                closestDistance = distance;
            }
        }
    }
    closestDistance = Integer.MAX_VALUE;
    for (Unit u2 : pgs.getUnits()) {
        if (u2.getType().isStockpile && u2.getPlayer() == player.getID()) {
            int distance = Math.abs(u2.getX() - worker.getX()) + Math.abs(u2.getY() - worker.getY());
            if (closestBase == null || distance < closestDistance) {
                closestBase = u2;
                closestDistance = distance;
            }
        }
    }

    if (worker.getResources() > 0) {
        if (closestBase != null) {
            deliverResources(worker, closestBase);
        }
    } else {
        if (closestResource != null && closestBase != null) {
            harvest(worker, closestResource, closestBase);
        }
    }
}

private void deliverResources(Unit worker, Unit base) {
    // Method for handling resource delivery by a worker
    
}

@Override
public List<ParameterSpecification> getParameters() {
    List<ParameterSpecification> params = new ArrayList<>();
    params.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));
    return params;
=======
package mins_bot;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.ParameterSpecification;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.*;

public class ShayAI extends AbstractionLayerAI {

    private Random randomGenerator = new Random();
    private UnitTypeTable unitTypeTable;
    UnitType workerUnit;
    UnitType baseUnit;
    UnitType barracksUnit;
    UnitType lightInfantry;
    UnitType heavyInfantry;


    public ShayAI(UnitTypeTable utt) {
        this(utt, new AStarPathFinding());
    }

    public ShayAI(UnitTypeTable utt, PathFinding pathFinder) {
        super(pathFinder);
        initializeTypes(utt);
    }

    private void initializeTypes(UnitTypeTable utt) {
        this.unitTypeTable = utt;
        workerUnit = unitTypeTable.getUnitType("Worker");
        baseUnit = unitTypeTable.getUnitType("Base");
        barracksUnit = unitTypeTable.getUnitType("Barracks");
        lightInfantry = unitTypeTable.getUnitType("Light");
        heavyInfantry = unitTypeTable.getUnitType("Heavy");
    }

    @Override
    public void reset() {
        super.reset();
    }

    public void reset(UnitTypeTable utt) {
        initializeTypes(utt);
    }

    @Override
    public AI clone() {
        return new ShayAI(unitTypeTable, pf);
    }
    
public PlayerAction getAction(int player, GameState gameState) {
    PhysicalGameState physicalState = gameState.getPhysicalGameState();
    Player currentPlayer = gameState.getPlayer(player);

    // Managing base actions
    for (Unit unit : physicalState.getUnits()) {
        if (unit.getType() == baseUnit && unit.getPlayer() == player && gameState.getActionAssignment(unit) == null) {
            handleBaseActions(unit, currentPlayer, physicalState);
        }
    }

    // Managing barracks actions
    for (Unit unit : physicalState.getUnits()) {
        if (unit.getType() == barracksUnit && unit.getPlayer() == player && gameState.getActionAssignment(unit) == null) {
            handleBarracksActions(unit, currentPlayer, physicalState);
        }
    }

    // Managing actions of attack units
    for (Unit unit : physicalState.getUnits()) {
        if (unit.getType().canAttack && !unit.getType().canHarvest && unit.getPlayer() == player && gameState.getActionAssignment(unit) == null) {
            handleAttackUnitActions(gameState, physicalState, currentPlayer);
        }
    }

    // Managing worker units
    List<Unit> workerUnits = new LinkedList<>();
    for (Unit unit : physicalState.getUnits()) {
        if (unit.getType().canHarvest && unit.getPlayer() == player) {
            workerUnits.add(unit);
        }
    }
    handleWorkerActions(workerUnits, currentPlayer, gameState);

    // Consolidating actions into a PlayerAction
    return translateActions(player, gameState);
}

private void handleBaseActions(Unit base, Player player, PhysicalGameState pgs) {
    int workerCount = 0;
    for (Unit unit : pgs.getUnits()) {
        if (unit.getType() == workerUnit && unit.getPlayer() == player.getID()) {
            workerCount++;
        }
    }
    if (workerCount < 3 && player.getResources() >= workerUnit.cost) {
        train(base, workerUnit);
    }
}

private void constructBuilding(Unit worker, UnitType buildingType, int x, int y, List<Integer> reservedLocations, Player player, PhysicalGameState pgs) {
    // Check if the position is already reserved or occupied
    if (reservedLocations.contains(x + y * pgs.getWidth()) || pgs.getUnitAt(x, y) != null) {
        // Find a new location if the current one is not available
        int newLocation = findBuildingLocation(x, y, reservedLocations, pgs);
        if (newLocation != -1) {
            x = newLocation % pgs.getWidth();
            y = newLocation / pgs.getWidth();
        } else {
            // No suitable location found, return without building
            return;
        }
    }

    // Reserve the location
    reservedLocations.add(x + y * pgs.getWidth());

    // Command the worker to build
    build(worker, buildingType, x, y);
}

private int findBuildingLocation(int startX, int startY, List<Integer> reservedLocations, PhysicalGameState pgs) {
    // Search for a suitable location around the starting point
    for (int x = startX - 2; x <= startX + 2; x++) {
        for (int y = startY - 2; y <= startY + 2; y++) {
            if (x >= 0 && y >= 0 && x < pgs.getWidth() && y < pgs.getHeight() && pgs.getUnitAt(x, y) == null) {
                int location = x + y * pgs.getWidth();
                if (!reservedLocations.contains(location)) {
                    return location;
                }
            }
        }
    }
    return -1; // No suitable location found
}

private void handleBarracksActions(Unit barracks, Player player, PhysicalGameState pgs) {
    // Count existing light and heavy infantry units
    int lightInfantryCount = 0;
    int heavyInfantryCount = 0;
    for (Unit u : pgs.getUnits()) {
        if (u.getType() == lightInfantry && u.getPlayer() == player.getID()) {
            lightInfantryCount++;
        }
        if (u.getType() == heavyInfantry && u.getPlayer() == player.getID()) {
            heavyInfantryCount++;
        }
    }

    // Decide whether to train light or heavy infantry based on a simple strategy
    boolean shouldTrainLightInfantry = lightInfantryCount <= heavyInfantryCount;

    // Check resources and train units accordingly
    if (shouldTrainLightInfantry && player.getResources() >= lightInfantry.cost) {
        train(barracks, lightInfantry);
    } else if (player.getResources() >= heavyInfantry.cost) {
        train(barracks, heavyInfantry);
    }
}

private void handleAttackUnitActions(GameState gs, PhysicalGameState pgs, Player player) {
    List<Unit> lightInfantryUnits = new ArrayList<>();
    List<Unit> heavyInfantryUnits = new ArrayList<>();

    // Separate light and heavy infantry units
    for (Unit u : pgs.getUnits()) {
        if (u.getPlayer() == player.getID()) {
            if (u.getType() == lightInfantry) {
                lightInfantryUnits.add(u);
            } else if (u.getType() == heavyInfantry) {
                heavyInfantryUnits.add(u);
            }
        }
    }

    // Form groups and assign targets
    while (lightInfantryUnits.size() >= 2 && heavyInfantryUnits.size() >= 1) {
        List<Unit> group = new ArrayList<>();
        group.add(lightInfantryUnits.remove(0));
        group.add(lightInfantryUnits.remove(0));
        group.add(heavyInfantryUnits.remove(0));

        Unit target = findNearestEnemy(group, pgs, player);
        if (target != null) {
            for (Unit unit : group) {
                attack(unit, target);
            }
        }
    }

    //ranged unit if needed.
}

private Unit findNearestEnemy(List<Unit> group, PhysicalGameState pgs, Player player) {
    Unit nearestEnemy = null;
    int minDistance = Integer.MAX_VALUE;
    for (Unit u : pgs.getUnits()) {
        if (u.getPlayer() >= 0 && u.getPlayer() != player.getID()) {
            for (Unit groupUnit : group) {
                int distance = Math.abs(u.getX() - groupUnit.getX()) + Math.abs(u.getY() - groupUnit.getY());
                if (nearestEnemy == null || distance < minDistance) {
                    nearestEnemy = u;
                    minDistance = distance;
                }
            }
        }
    }
    return nearestEnemy;
}
private void handleWorkerActions(List<Unit> workers, Player player, GameState gs) {
    PhysicalGameState pgs = gs.getPhysicalGameState();
    int baseCount = 0;
    int barracksCount = 0;

    int usedResources = 0;
    List<Unit> availableWorkers = new LinkedList<>(workers);

    if (workers.isEmpty()) {
        return;
    }

    // Counting existing bases and barracks
    for (Unit u : pgs.getUnits()) {
        if (u.getType() == baseUnit && u.getPlayer() == player.getID()) {
            baseCount++;
        }
        if (u.getType() == barracksUnit && u.getPlayer() == player.getID()) {
            barracksCount++;
        }
    }

    List<Integer> reservedLocations = new LinkedList<>();
    if (baseCount == 0 && !availableWorkers.isEmpty()) {
        // Building a base
        if (player.getResources() >= baseUnit.cost + usedResources) {
            Unit worker = availableWorkers.remove(0);
            constructBuilding(worker, baseUnit, worker.getX(), worker.getY(), reservedLocations, player, pgs);
            usedResources += baseUnit.cost;
        }
    }

    if (barracksCount == 0) {
        // Building a barracks
        if (player.getResources() >= barracksUnit.cost + usedResources && !availableWorkers.isEmpty()) {
            Unit worker = availableWorkers.remove(0);
            constructBuilding(worker, barracksUnit, worker.getX(), worker.getY(), reservedLocations, player, pgs);
            usedResources += barracksUnit.cost;
        }
    }

    // Assigning remaining workers to harvest resources
    for (Unit worker : availableWorkers) {
        manageResourceHarvesting(worker, pgs, player);
    }
}

private void manageResourceHarvesting(Unit worker, PhysicalGameState pgs, Player player) {
    Unit closestBase = null;
    Unit closestResource = null;
    int closestDistance = Integer.MAX_VALUE;
    for (Unit u2 : pgs.getUnits()) {
        if (u2.getType().isResource) {
            int distance = Math.abs(u2.getX() - worker.getX()) + Math.abs(u2.getY() - worker.getY());
            if (closestResource == null || distance < closestDistance) {
                closestResource = u2;
                closestDistance = distance;
            }
        }
    }
    closestDistance = Integer.MAX_VALUE;
    for (Unit u2 : pgs.getUnits()) {
        if (u2.getType().isStockpile && u2.getPlayer() == player.getID()) {
            int distance = Math.abs(u2.getX() - worker.getX()) + Math.abs(u2.getY() - worker.getY());
            if (closestBase == null || distance < closestDistance) {
                closestBase = u2;
                closestDistance = distance;
            }
        }
    }

    if (worker.getResources() > 0) {
        if (closestBase != null) {
            deliverResources(worker, closestBase);
        }
    } else {
        if (closestResource != null && closestBase != null) {
            harvest(worker, closestResource, closestBase);
        }
    }
}

private void deliverResources(Unit worker, Unit base) {
    // Method for handling resource delivery by a worker
    
}

@Override
public List<ParameterSpecification> getParameters() {
    List<ParameterSpecification> params = new ArrayList<>();
    params.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));
    return params;
}
>>>>>>> b47aaa4adc71a01ab1df3aef91ca2f8ad766db49
}