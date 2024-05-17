/**
 * @file Miguel_MINS_Bot.java
 * @author Miguel Quemado <mq003322@ohio.edu>
 * @brief This class is an implementation of a microRTS bot.
 * 
 * This version of mins_bot is Miguel's. My strategy is to combine the ideas of a light rush and proxy rush where I will initially gain resources quickly, then as soon as possible begin building a 
 * barrack close to the enemy's structures and rush light units while continually gathering resources. The idea behind this strategy is to quickly distract and overwhelm the opposition before they
 * can establish a strong attack and overwhelm them during their preperations. This strategy hinges on the success of the initial proxy build and attack. If it fails, my bot most likely loses.
 */

package miguel_mins_bot;

import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import java.util.ArrayList;
import java.util.List;
import rts.GameState;
import static rts.PhysicalGameState.TERRAIN_WALL;
import rts.Player;
import rts.PlayerAction;
import rts.UnitAction;
import rts.UnitActionAssignment;
import rts.units.Unit;
import rts.units.UnitTypeTable;
import ai.abstraction.pathfinding.AStarPathFinding;

// This version of mins_bot is Miguel's. My strategy is to combine the ideas of a light rush and proxy rush where I will initially gain resources quickly, then as soon as possible begin building a 
// barrack close to the enemy's structures and rush light units while continually gathering resources. The idea behind this strategy is to quickly distract and overwhelm the opposition before they
// can establish a strong attack and overwhelm them during their preperations. This strategy hinges on the success of the initial proxy build and attack. If it fails, my bot most likely loses.

public class Miguel_MINS_Bot extends AIWithComputationBudget {

    /**
     * @brief This class represents a coordinate location on the board (x,y)
     */
    public class Pos {
        int _x;
        int _y;

        /**
         * @brief Construct a Pos object
         * 
         * @param x coordinate location 'x'
         * @param y coordinate location 'y'
         */
        Pos(int x, int y) {
            _x = x;
            _y = y;
        }
        public int getX() { return _x; }
        public int getY() { return _y; }
    }

    UnitTypeTable m_utt = null;
    PlayerAction playerAction;
    GameState gameState;
    Player _player;
    int playerID;
    AStarPathFinding pathFinder;

    // Friendly units
    List<Unit> bases;
    List<Unit> barracks;
    List<Unit> workers;
    List<Unit> lights;
    List<Unit> allyUnits;
    Pos proxyPos;

    // might implement later, lists enemies that can attack. List<Unit> _allyCombat;

    // Enemy units
    List<Unit> enemyBases;
    List<Unit> enemyBarracks;
    List<Unit> enemyWorkers;
    List<Unit> enemyHeavies;
    List<Unit> enemyRanged;
    List<Unit> enemyLights;
    List<Unit> enemies;
    // might implement later, lists enemies that can attack. List<Unit> _enemiesCombat;

    List<Unit> all;
    List<Unit> resources;

    /**
     * @brief This is the default constructor that microRTS will call
     * 
     * @param utt
     */
    public Miguel_MINS_Bot(UnitTypeTable utt) {
        super(-1,-1);
        pathFinder = new AStarPathFinding();
        m_utt = utt;

    }

    @Override
    public List<ParameterSpecification> getParameters() {
        return new ArrayList<>();
    }

    @Override
    public AI clone() {
        return new Miguel_MINS_Bot(m_utt);
    }

    // This will be called once at the beginning of each new game:    
    @Override
    public void reset() {
    }

    /**
     * @brief Checks if a given unit is an enemy
     * 
     * @param u unit to check if enemy or not
     * @return true if unit is an enemy, false otherwise
     */
    boolean isEnemyUnit(Unit u) {
        return u.getPlayer() >= 0 && u.getPlayer() != _player.getID(); //can be neither ally or foe
    }

    /**
     * @brief Initialize the class's attributes from the current game state
     */
    public void init() {

        allyUnits = new ArrayList<>(); 
        bases = new ArrayList<>();
        barracks = new ArrayList<>();
        workers = new ArrayList<>();
        lights = new ArrayList<>();

        enemies  = new ArrayList<>();
        enemyBases = new ArrayList<>();
        enemyBarracks = new ArrayList<>();
        enemyWorkers = new ArrayList<>();
        enemyHeavies = new ArrayList<>();
        enemyRanged = new ArrayList<>();
        enemyLights = new ArrayList<>();

        all = new ArrayList<>();
        resources = new ArrayList<>();
        
        for (Unit u : gameState.getPhysicalGameState().getUnits()) {
            if (u.getType().isResource)
                resources.add(u);
            else if (u.getType() == m_utt.getUnitType("Base") && isEnemyUnit(u))
                enemyBases.add(u);
            else if (u.getType() == m_utt.getUnitType("Base"))
                bases.add(u);
            else if (u.getType() == m_utt.getUnitType("Barracks") && isEnemyUnit(u))
                enemyBarracks.add(u);
            else if (u.getType() == m_utt.getUnitType("Barracks"))
                barracks.add(u);
            else if (u.getType() == m_utt.getUnitType("Worker") && isEnemyUnit(u))
                enemyWorkers.add(u);
            else if (u.getType() == m_utt.getUnitType("Worker"))
                workers.add(u);
            else if (u.getType() == m_utt.getUnitType("Ranged") && isEnemyUnit(u))
                enemyRanged.add(u);
            else if (u.getType() == m_utt.getUnitType("Heavy") && isEnemyUnit(u))
                enemyHeavies.add(u);
            else if (u.getType() == m_utt.getUnitType("Light") && isEnemyUnit(u))
                enemyLights.add(u);
            else if (u.getType() == m_utt.getUnitType("Light"))
                lights.add(u);     
        }

        for (Unit u : gameState.getPhysicalGameState().getUnits()) {
            if(u.getType().isResource)
                continue;
            all.add(u);
            if (isEnemyUnit(u))
                enemies.add(u);
            else
                allyUnits.add(u);
        }

        // Get proxy location
        Unit closestEnemyBase;
        Unit closestEnemyBarracks;
        Unit closestEnemyStructure;

        if (!enemyBases.isEmpty() && enemyBarracks.isEmpty()) {
            closestEnemyStructure = getClosestEnemyType(enemyBases.get(0), "Base");
            int enemyStructureX = closestEnemyStructure.getX();
            int enemyStructureY = closestEnemyStructure.getY();
            proxyPos = getBuildLocation(enemyStructureX, enemyStructureY, gameState.getPhysicalGameState().getWidth(), gameState.getPhysicalGameState().getHeight());
        }
        else if (enemyBases.isEmpty() && !enemyBarracks.isEmpty()) {
            closestEnemyStructure = getClosestEnemyType(enemyBarracks.get(0), "Barracks");
            int enemyStructureX = closestEnemyStructure.getX();
            int enemyStructureY = closestEnemyStructure.getY();
            proxyPos = getBuildLocation(enemyStructureX, enemyStructureY, gameState.getPhysicalGameState().getWidth(), gameState.getPhysicalGameState().getHeight());
        }
        else if (!enemyBases.isEmpty() && !enemyBarracks.isEmpty()) {
            closestEnemyBase = getClosestEnemyType(enemyBases.get(0), "Base");
            closestEnemyBarracks = getClosestEnemyType(enemyBarracks.get(0), "Barracks");
            if (getSquareUnitDistance(enemyBases.get(0), closestEnemyBase) < getSquareUnitDistance(enemyBases.get(0), closestEnemyBarracks)) {
                closestEnemyStructure = closestEnemyBase;
            }
            else {
                closestEnemyStructure = closestEnemyBarracks;
            }

            int enemyStructureX = closestEnemyStructure.getX();
            int enemyStructureY = closestEnemyStructure.getY();
            proxyPos = getBuildLocation(enemyStructureX, enemyStructureY, gameState.getPhysicalGameState().getWidth(), gameState.getPhysicalGameState().getHeight());
        }
        else {
            proxyPos = null;
        }
    }

    /**
     * @brief Called by microRTS at each game cycle, gets a player's action
     * 
     * @param player int to represent which player
     * @param gs current game state
     * @return The action the bot wants to execute
     */
    @Override
    public PlayerAction getAction(int player, GameState gs) {
        gameState = gs.clone();
        playerID = player;
        _player = gameState.getPlayer(player);
        playerAction = new PlayerAction();
        playerAction.clear();

        init();

        for(Unit u : allyUnits) {                                               // goes through each unit and calls the relevent function to set its action if needed
            
            if(!u.isIdle(gameState) || u.getPlayer() != player) {               // if the unit is occupied or controlled by the other player go to the next unit
                continue;
            }
            
            // calls the function for the current unit
            if("Worker".equals(u.getType().name)) {
                setWorkerAction(u);
            }
            
            if("Light".equals(u.getType().name)) {
                setLightAction(u);
            }
            
            if("Barracks".equals(u.getType().name)) {
                setBarracksAction(u);
            }
            
            if("Base".equals(u.getType().name)) {
                setBaseAction(u);
            }

            gameState.issue(playerAction);                  //issues the actions assigned so far to allow conflicting actions to be caught
            
        }
        
        playerAction.fillWithNones(gameState, player, 1);   //fills any remaining actions with a 1 frame none actions (a unit must always have an action assigned to it)
                
        return playerAction;
    }    

    /**
     * @brief Choose an action for a base unit
     * 
     * @param u base unit for which to decide action
     */
    public void setBaseAction(Unit u) {

            UnitAction produceWorker;
            int workerCount = workers.size();
            if (_player.getResources() >= 5 && workerCount < 3) {   // if we have 5 or more resources && less than 3 workers, then make a worker. The values can be modified as we test.

                
                // Check free spaces, if none, then wait
                int xPos = u.getX();
                int yPos = u.getY();
                if (posFree(xPos, yPos, UnitAction.DIRECTION_DOWN))
                    produceWorker = new UnitAction(UnitAction.TYPE_PRODUCE, UnitAction.DIRECTION_DOWN, m_utt.getUnitType("Worker"));
                else if (posFree(xPos, yPos, UnitAction.DIRECTION_UP))
                    produceWorker = new UnitAction(UnitAction.TYPE_PRODUCE, UnitAction.DIRECTION_UP, m_utt.getUnitType("Worker"));
                else if (posFree(xPos, yPos, UnitAction.DIRECTION_LEFT))
                    produceWorker = new UnitAction(UnitAction.TYPE_PRODUCE, UnitAction.DIRECTION_LEFT, m_utt.getUnitType("Worker"));
                else if (posFree(xPos, yPos, UnitAction.DIRECTION_RIGHT))
                    produceWorker = new UnitAction(UnitAction.TYPE_PRODUCE, UnitAction.DIRECTION_RIGHT, m_utt.getUnitType("Worker"));
                else
                    produceWorker = new UnitAction(UnitAction.TYPE_NONE);
            } else {
                produceWorker = new UnitAction(UnitAction.TYPE_NONE);
            }

        if (produceWorker!= null && gameState.isUnitActionAllowed(u, produceWorker)) {
            playerAction.addUnitAction(u, produceWorker);
        }
        else {
            playerAction.addUnitAction(u, new UnitAction(UnitAction.TYPE_NONE));
        }
    }

    /**
     * @brief Choose an action for a barracks unit
     * 
     * @param u barracks unit for which to decide action
     */
    public void setBarracksAction(Unit u) {
        
        /* since the bot will only have one barracks on the board at a time for now, this isn't necessary. Might be changed later

        int idleBarracks = 0;
        for(Unit currentUnit:units) {           // finds how many idle barracks there are to consider the money that should be used
            if(currentUnit.isIdle(gameState) && currentUnit.getPlayer() == playerID && currentUnit.getType().name.equals("Barracks")) {
                ++idleBarracks;
            }
        }
        int budget = (gameState.getPhysicalGameState().getPlayer(playerID).getResources() - playerAction.getResourceUsage().getResourcesUsed(playerID)) / idleBarracks; // the share of this barracks resources in evenly split amoung idle barracks. Ignores worker and base resource usage.
        
        */

        UnitAction produceUnit;
        if (_player.getResources() >= 2) {

            // Check free spaces, if none, then wait
            int xPos = u.getX();
            int yPos = u.getY();
            if (posFree(xPos, yPos, UnitAction.DIRECTION_DOWN))
                produceUnit = new UnitAction(UnitAction.TYPE_PRODUCE, UnitAction.DIRECTION_DOWN, m_utt.getUnitType("Light"));
            else if (posFree(xPos, yPos, UnitAction.DIRECTION_UP))
                produceUnit = new UnitAction(UnitAction.TYPE_PRODUCE, UnitAction.DIRECTION_UP, m_utt.getUnitType("Light"));
            else if (posFree(xPos, yPos, UnitAction.DIRECTION_LEFT))
                produceUnit = new UnitAction(UnitAction.TYPE_PRODUCE, UnitAction.DIRECTION_LEFT, m_utt.getUnitType("Light"));
            else if (posFree(xPos, yPos, UnitAction.DIRECTION_RIGHT))
                produceUnit = new UnitAction(UnitAction.TYPE_PRODUCE, UnitAction.DIRECTION_RIGHT, m_utt.getUnitType("Light"));
            else
                produceUnit = new UnitAction(UnitAction.TYPE_NONE);
        }
        else {  // If not, do nothing
            produceUnit = new UnitAction(UnitAction.TYPE_NONE);
        }
        
        if(produceUnit != null && gameState.isUnitActionAllowed(u, produceUnit)) {
            playerAction.addUnitAction(u, produceUnit);
        }
        else {
            playerAction.addUnitAction(u, new UnitAction(UnitAction.TYPE_NONE));
        }
    }
    
    /**
     * @brief Choose an action for a worker unit
     * 
     * @param u worker unit for which to decide action
     */
    public void setWorkerAction(Unit u) {

        UnitAction workerAction;

		if(bases.isEmpty())return;
        Unit mainBase = bases.get(0);

        // Locate nearest resource unit here
        Unit closestResource = null;
        int distance = gameState.getPhysicalGameState().getWidth()*gameState.getPhysicalGameState().getWidth(); // Max possible distance from one corner to the other
        for (Unit r : resources) {
            int checkDist = getSquareUnitDistance(u, r);
            if (checkDist < distance) {
                distance = checkDist;
                closestResource = r;
            }
        }

        int buildPosX, buildPosY;
        int movePosX, movePosY;

        if (proxyPos != null) {
            buildPosX = proxyPos.getX();
            buildPosY = proxyPos.getY();
            movePosX = buildPosX + 1;
            movePosY = buildPosY;
        }
        else {
            buildPosX = gameState.getPhysicalGameState().getWidth()/2;
            buildPosY = gameState.getPhysicalGameState().getHeight()/2;
            movePosX = buildPosX + 1;
            movePosY = buildPosY;
        }

        System.out.println(buildPosX + ", " + buildPosY);

        if(busy(u)) {
            System.out.println("Unit is busy");
            return;
        }
        else if (proxyPos != null && _player.getResources() >= 6 && barracks.size() < 1) {     
            // find location to build barracks
            // move there and build it

            // Find the nearest enemy structure, either a base or a barracks
            
            // Build the proxy barracks
            if (u.getX() == movePosX && u.getY() == movePosY) {  // If in the move location that is suitable for building, then build
                if (gameState.getPhysicalGameState().getUnitAt(buildPosX, buildPosY) == null) {     // Check if the space to build is free and no other worker is already going to build.
                    workerAction = new UnitAction(UnitAction.TYPE_PRODUCE, UnitAction.DIRECTION_UP, m_utt.getUnitType("Barracks"));
                }
                else {  // Otherwise, wait
                    workerAction = new UnitAction(UnitAction.TYPE_NONE);
                }
            }
            else {  // Otherwise, move to move location
                workerAction = moveTowardsPosition(u, movePosX, movePosY);
            }
        }
        else if (resources.isEmpty() && !enemyBases.isEmpty()) {                    // Attack nearest enemy base
            Unit closestEnemyBase = getClosestEnemyType(u, "Base");
            if (isInRange(u, closestEnemyBase)) {                                   // If in range, attack
                workerAction = new UnitAction(UnitAction.TYPE_ATTACK_LOCATION, closestEnemyBase.getX(), closestEnemyBase.getY());
            }
            else {                                                                  // Otherwise, move in range
                workerAction = moveInRange(u, closestEnemyBase);
            }
        }
        else if (resources.isEmpty() && !enemyBarracks.isEmpty()) {                 // Attack nearest enemy barracks
            Unit closestEnemyBarracks = getClosestEnemyType(u, "Barracks");
            if (isInRange(u, closestEnemyBarracks)) {                               // If in range, attack
                workerAction = new UnitAction(UnitAction.TYPE_ATTACK_LOCATION, closestEnemyBarracks.getX(), closestEnemyBarracks.getY());
            }
            else {                                                                  // Otherwise, move in range
                workerAction = moveInRange(u, closestEnemyBarracks);
            }  
        }
        else if (resources.isEmpty() && !enemyRanged.isEmpty()) {                   // Attack nearest enemy combatant
            Unit closestEnemyCombatant = getClosestEnemyType(u, "Ranged");
            if (isInRange(u, closestEnemyCombatant)) {                              // If in range, attack
                workerAction = new UnitAction(UnitAction.TYPE_ATTACK_LOCATION, closestEnemyCombatant.getX(), closestEnemyCombatant.getY());
            }
            else {                                                                  // Otherwise, move in range
                workerAction = moveInRange(u, closestEnemyCombatant);
            }
        }
        else if (resources.isEmpty() && !enemyHeavies.isEmpty()) {
            Unit closestEnemyCombatant = getClosestEnemyType(u, "Heavy");
            if (isInRange(u, closestEnemyCombatant)) {                              // If in range, attack
                workerAction = new UnitAction(UnitAction.TYPE_ATTACK_LOCATION, closestEnemyCombatant.getX(), closestEnemyCombatant.getY());
            }
            else {                                                                  // Otherwise, move in range
                workerAction = moveInRange(u, closestEnemyCombatant);
            }
        }
        else if (resources.isEmpty() && !enemyLights.isEmpty()) {
            Unit closestEnemyCombatant = getClosestEnemyType(u, "Light");
            if (isInRange(u, closestEnemyCombatant)) {                              // If in range, attack
                workerAction = new UnitAction(UnitAction.TYPE_ATTACK_LOCATION, closestEnemyCombatant.getX(), closestEnemyCombatant.getY());
            }
            else {                                                                  // Otherwise, move in range
                workerAction = moveInRange(u, closestEnemyCombatant);
            }
        }
        else if (resources.isEmpty() && !enemyWorkers.isEmpty()) {
            Unit closestEnemyCombatant = getClosestEnemyType(u, "Worker");
            if (isInRange(u, closestEnemyCombatant)) {                              // If in range, attack
                workerAction = new UnitAction(UnitAction.TYPE_ATTACK_LOCATION, closestEnemyCombatant.getX(), closestEnemyCombatant.getY());
            }
            else {                                                                  // Otherwise, move in range
                workerAction = moveInRange(u, closestEnemyCombatant);
            }
        }
        else {                                                                      // Gather nearest resource
            if (closestResource == null) {                                          // Do nothing if no closest resources
                workerAction = new UnitAction(UnitAction.TYPE_NONE);
            }
            else {                                                                  // Harvest resource
                if (u.getResources() != 0) {                                        // If carrying resource, go return it
                    if (isInRange(u, mainBase)) {                                   // If next to base, deposit resource
                        workerAction = new UnitAction(UnitAction.TYPE_RETURN, toDir(toPos(u), toPos(mainBase)));
                    }
                    else {                                                          // Otherwise, move to base
                        workerAction = moveInRange(u, mainBase);
                    }
                }
                else if (isInRange(u, closestResource)) {                           // If in range, harvest
                    //commented out for demo System.out.println("harvest action added");
                    workerAction = new UnitAction(UnitAction.TYPE_HARVEST, toDir(toPos(u), toPos(closestResource)));
                }
                else {                                                              // Otherwise, move in range
                    //commented out for demo System.out.println("moveInRange");
                    workerAction = moveInRange(u, closestResource);
                }
            }
        }

        if(workerAction != null && gameState.isUnitActionAllowed(u, workerAction)) {
            playerAction.addUnitAction(u, workerAction);
            return;
        }
        playerAction.addUnitAction(u, new UnitAction(UnitAction.TYPE_NONE));
        return;
    }
    
    /**
     * @brief Choose an action for a light unit
     * 
     * @param u light unit for which to decide action
     */
    public void setLightAction(Unit u) {        
        List<UnitAction> validActions = u.getUnitActions(gameState, 1); //gets all actions the unit can perform
        Unit closestEnemy = getClosestEnemy(u);

        if (closestEnemy != null) 
        {   // Prioritize attacking closest enemy first        
            UnitAction attackAction = new UnitAction(UnitAction.TYPE_ATTACK_LOCATION, closestEnemy.getX(), closestEnemy.getY()); 
            if (attackAction != null && validActions.contains(attackAction) && gameState.isUnitActionAllowed(u, attackAction)) 
            {
                playerAction.addUnitAction(u, attackAction);
                return;
            }
        }
       
        // Otherwise, move to find an enemy to attack
        if (closestEnemy != null)
        {
            UnitAction moveTowardsClosest = moveInRange(u, closestEnemy);
            if(moveTowardsClosest != null)
            {
                 playerAction.addUnitAction(u, moveTowardsClosest);
            }
        }
    }

    /**
     * @brief Check if a unit is busy carrying out an action
     * 
     * @param u unit to check busy status
     * @return true if busy, false if otherwise
     */
    boolean busy(Unit u) {
        if(playerAction.getAction(u) != null)
            return true;
        UnitActionAssignment aa = gameState.getActionAssignment(u);
        return aa != null;
    }

    /**
     * @brief Find the closest enemy unit
     * 
     * @param u unit to check it's proximity to the enemy
     * @return the enemy closest to the unit u or null if there is none
     */
    private Unit getClosestEnemy(Unit u) {
        Unit closestEnemy = null;                                           // Checks for any enemy units nearby the heavy units
        int closestDistance = Integer.MAX_VALUE;
        for (Unit unit : gameState.getUnits()) 
        {
            if (unit.getPlayer() >= 0 && unit.getPlayer() != u.getPlayer()) 
            { 
                int distance = getSquareUnitDistance(u, unit);
                if (distance < closestDistance) 
                {
                    closestDistance = distance;
                    closestEnemy = unit;
                }
            }
        }
        return closestEnemy;
    }
    
    /**
     * @brief find the square distance between units
     * 
     * @param u unit to find distance between
     * @param other other unit to find distance between
     * @return the integer square distance between the two units
     */
    private int getSquareUnitDistance(Unit u, Unit other) {
        return (other.getY() - u.getY())*(other.getY() - u.getY()) + (other.getX() - u.getX())*(other.getX() - u.getX());
    }

   /**
    * @brief Fetch a move action to move a unit within range to attack 
    *
    * @param u unit to move
    * @param other unit to move within attack range
    * @return UnitAction move action to move within attack range, null if no path exists
    */
    private UnitAction moveInRange(Unit u, Unit other) {
        int width = gameState.getPhysicalGameState().getWidth();
        return pathFinder.findPathToPositionInRange(u, other.getX() + other.getY()*width, u.getAttackRange(), gameState, playerAction.getResourceUsage());
    }

   /**
    * @brief Fetch a move action to move to a position adjacent to another unit 
    *
    * @param u unit to move
    * @param other target unit
    * @return UnitAction move action to move to adjacent position, null if no path exists
    */
    private UnitAction moveTowardsUnit(Unit u, Unit other) {
        int width = gameState.getPhysicalGameState().getWidth();
        return pathFinder.findPathToAdjacentPosition(u, other.getX() + other.getY()*width, gameState, playerAction.getResourceUsage());
    }
    
   /**
    * @brief Fetch a move action to move to a specified position on the board
    *
    * @param u unit to move
    * @param x coordinate 'x' of target destination
    * @param y coordinate 'y' of target destination
    * @return UnitAction move action to move to specified position, null if no path exists
    */
    private UnitAction moveTowardsPosition(Unit u, int x, int y) {
        int width = gameState.getPhysicalGameState().getWidth();
        return pathFinder.findPath(u, x + y*width, gameState, playerAction.getResourceUsage());
    }

    /**
     * @brief Check if a given position (x, y) is free/not occupied
     * 
     * @param x coordinate 'x' position to check
     * @param y coordinate 'y' position to check
     * @param dir direction to check
     * @return true if free, false otherwise
     */
    public boolean posFree(int x, int y, int dir) {
        Pos pos = futurePos(x, y, dir);
        if(gameState.getPhysicalGameState().getUnitAt(pos.getX(), pos.getY()) != null)  // check to see if there's a unit there
            return false;
        if (gameState.getPhysicalGameState().getTerrain(pos.getX(), pos.getY()) == TERRAIN_WALL)    // check to see if it's the wall
            return false;
        return true;
    }

    /**
     * @brief Fetch a future position given a direction and starting position
     * 
     * @param x coordinate 'x' position to fetch from
     * @param y coordinate 'y' position to fetch from
     * @param dir direction
     * @return Pos object to fetch
     */
    Pos futurePos(int x, int y, int dir) {
        int nx = x;
        int ny = y;
        switch (dir) {
            case UnitAction.DIRECTION_DOWN:
                ny = (ny == gameState.getPhysicalGameState().getHeight()- 1) ? ny : ny + 1;
                break;
            case UnitAction.DIRECTION_UP:
                ny = (ny == 0) ? ny : ny - 1;
                break;
            case UnitAction.DIRECTION_RIGHT:
                nx = (nx == gameState.getPhysicalGameState().getWidth() - 1) ? nx : nx + 1;
                break;
            case UnitAction.DIRECTION_LEFT:
                nx = (nx == 0) ? nx : nx - 1;
                break;
            default:
                break;
        }
        return new Pos(nx, ny);
    }

    /**
     * @brief Fetch a given unit's position represented as a Pos object
     * 
     * @param u unit whose position to fetch
     * @return Pos object representing u's position
     */
    Pos toPos(Unit u) {
        return new Pos(u.getX(), u.getY());
    }

    /**
     * @brief Fetch the closest enemy by unit type relative to a given unit
     * 
     * @param u unit from which we search
     * @param type unit type to find
     * @return Unit closest enemy unit of specified type, or itself, 'u' if no such enemy unit
     */
    public Unit getClosestEnemyType(Unit u, String type) {

        List<Unit> unitOfType = new ArrayList<>();
        switch (type) {
            case "Base":
                unitOfType.addAll(enemyBases);
                break;
            case "Barracks":
                unitOfType.addAll(enemyBarracks);
                break;
            case "Worker":
                unitOfType.addAll(enemyWorkers);
                break;
            case "Light":
                unitOfType.addAll(enemyLights);
                break;
            case "Heavy":
                unitOfType.addAll(enemyHeavies);
                break;
            case "Ranged":
                unitOfType.addAll(enemyRanged);
                break;
            default:
                System.out.println("Invalid enemy type was attempted");
        }

        if (unitOfType.size() == 0) {   // Check if there are no units of type 'type' or if the type was incorrect
            return u;                   // Return itself
        }

        Unit closestEnemy = null;
        int closestDistance = Integer.MAX_VALUE;
        for (Unit unit : unitOfType) {
            int distance = getSquareUnitDistance(u, unit);
            if (distance < closestDistance) 
            {
                closestDistance = distance;
                closestEnemy = unit;
            }
        }
        return closestEnemy;
    }

    /**
     * @brief Check if a unit is within range of attacking
     * 
     * @param src unit from which to check
     * @param target unit to check if it is in range of attack
     * @return true if in range, false otherwise
     */
    public boolean isInRange(Unit src, Unit target) {
        int srcX = src.getX();
        int srcY = src.getY();
        int tarX = target.getX();
        int tarY = target.getY();
        
        if (srcX == tarX) {
            if (srcY == tarY+1 || srcY == tarY-1)
                return true;
        }
        else if (srcY == tarY) {
            if (srcX == tarX+1 || srcX == tarX-1)
                return true;
        }

        return false;
    }

    int toDir(Pos src, Pos dst) {
        int dx = dst.getX() - src.getX();
        int dy = dst.getY() - src.getY();
        int dirX = dx > 0 ? UnitAction.DIRECTION_RIGHT : UnitAction.DIRECTION_LEFT;
        int dirY = dy > 0 ? UnitAction.DIRECTION_DOWN : UnitAction.DIRECTION_UP;
        if (Math.abs(dx) > Math.abs(dy))
            return dirX;
        return dirY;
     }

    /**
     * @brief Fetch the Pos object of the position of the proxy barracks build location
     * 
     * @param enemyX enemy building coordinate 'x' position
     * @param enemyY enemy building coordinate 'y' position
     * @param boardWidth board width
     * @param boardHeight board height
     * @return Pos representing build location
     */
    Pos getBuildLocation(int enemyX, int enemyY, int boardWidth, int boardHeight) {

        int proxyDistance = boardWidth/2+boardWidth/5;

        int buildX, buildY;

        // Check up/down/left/right
        if (enemyX - proxyDistance >= 0) {
            buildX = enemyX - proxyDistance;
            buildY = enemyY;
        }
        else if (enemyX + proxyDistance < boardWidth) {
            buildX = enemyX + proxyDistance;
            buildY = enemyY;
        }
        else if (enemyY - proxyDistance >= 0) {
            buildX = enemyX;
            buildY = enemyY - proxyDistance;
        }
        else if (enemyX + proxyDistance < boardHeight) {
            buildX = enemyX;
            buildY = enemyY + proxyDistance;
        }

        // Check corners
        else if (enemyX - proxyDistance >= 0 && enemyY - proxyDistance >= 0) {
            buildX = enemyX - proxyDistance;
            buildY = enemyY - proxyDistance;
        }
        else if (enemyX - proxyDistance >= 0 && enemyY + proxyDistance < boardHeight) {
            buildX = enemyX - proxyDistance;
            buildY = enemyY - proxyDistance;
        }

        else if (enemyX + proxyDistance < boardWidth && enemyY - proxyDistance >= 0) {
            buildX = enemyX - proxyDistance;
            buildY = enemyY - proxyDistance;
        }
        else if (enemyX + proxyDistance < boardWidth && enemyY + proxyDistance < boardHeight) {
            buildX = enemyX - proxyDistance;
            buildY = enemyY - proxyDistance;
        }
        else {
            buildX = -1;
            buildY = -1;
        }

        return new Pos(buildX, buildY); // Return invalid Pos
    }
}
