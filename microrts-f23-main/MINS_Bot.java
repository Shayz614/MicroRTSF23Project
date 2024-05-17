package mins_bot;
      
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import rts.GameState;
import rts.PlayerAction;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;
import java.lang.Math;
import ai.abstraction.pathfinding.AStarPathFinding;

public class MINS_Bot extends AIWithComputationBudget 
{

    UnitTypeTable m_utt = null;
    private long startTime; //Variable to track when the decision-making process starts
    private long maxTime;  //Variable to store the maximum allowed time for computation
    PlayerAction playerAction;
    GameState gameState;
    int playerID;
    AStarPathFinding pathFinder;
    // This is the default constructor that microRTS will call:

    public MINS_Bot(UnitTypeTable utt) 
    {

        super(-1,-1);
        pathFinder = new AStarPathFinding();
        m_utt = utt;
        maxTime = TIME_BUDGET; //Sets the maxTime to TIME_BUDGET

    }
    public AI clone() 
    {
        return new MINS_Bot(m_utt);
    }

    // This will be called once at the beginning of each new game:    
    public void reset() 
    {

    }

       

    // Called by microRTS at each game cycle.

    // Returns the action the bot wants to execute.

    public PlayerAction getAction(int player, GameState gs) 
    {
        gameState = gs.clone();
        playerID = player;
        playerAction = new PlayerAction();
        playerAction.clear();
        
        List<Unit> units = gameState.getUnits();
        for(Unit u : units){//goes through each unit and calls the relavent function to set its action if needed
            if(!u.isIdle(gameState) || u.getPlayer() != player)
            {//if the unit is occupied or controlled by the other player go to the next unit
                continue;
            }
            //calls the function for the current unit
            if("Worker".equals(u.getType().name))
            {
                //setWorkerAction(u);
            }
            
            if("Light".equals(u.getType().name))
            {
                setLightAction(u);
            }
            
            if("Heavy".equals(u.getType().name))
            {
                setHeavyAction(u);
            }
            
            if("Ranged".equals(u.getType().name))
            {
                setRangedAction(u);
            }
            
            if("Barracks".equals(u.getType().name))
            {
                setBarracksAction(u);
            }
            
            if("Base".equals(u.getType().name))
            {
                //setBaseAction(u);
            }
            gameState.issue(playerAction);//issues the actions assigned so far to allow conflicting actions to be caught
            
        }
        //fills any remaining actions with a 1 frame none actions (a unit must always have an action assigned to it)
        playerAction.fillWithNones(gameState, player, 1);
                
        return playerAction;
    }    
    
    //function for each unit type to decide the action the given unit should take and add it to playerActions
    public void setWorkerAction(Unit u)
    {   
        List<UnitAction> validActions = u.getUnitActions(gameState, 1);//gets all actions the unit can perform
        if(gameState.isUnitActionAllowed(u, validActions.get(0)))
        {//makes sure the action does not conflict with other unit actions
            playerAction.addUnitAction(u, validActions.get(0));//does whatever happens to be at index 0. This should be altered to perform what is found to be the best action
        }
    }
    
    
    public void setLightAction(Unit u)
    {        
        List<UnitAction> validActions = u.getUnitActions(gameState, 1);//gets all actions the unit can perform
        Unit closestEnemy = getClosestEnemy(u);
        int closestDistance = getSquareUnitDistance(u, closestEnemy);

        if (closestEnemy != null) 
        {// Prioritize attacking closest enemy first        
            UnitAction attackAction = new UnitAction(UnitAction.TYPE_ATTACK_LOCATION, closestEnemy.getX(), closestEnemy.getY()); 
            if (validActions.contains(attackAction) && gameState.isUnitActionAllowed(u, attackAction)) 
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
    
    // Heavy units Action set(Shay)
    public void setHeavyAction(Unit u)
    {
        List<UnitAction> validActions = u.getUnitActions(gameState, 1);//gets all actions the unit can perform
        Unit closestEnemy = getClosestEnemy(u);
        int closestDistance = getSquareUnitDistance(u, closestEnemy);

        if (closestEnemy != null) 
        {// Prioritize attacking closest enemy first        
            UnitAction attackAction = new UnitAction(UnitAction.TYPE_ATTACK_LOCATION, closestEnemy.getX(), closestEnemy.getY()); 
            if (validActions.contains(attackAction) && gameState.isUnitActionAllowed(u, attackAction)) 
            {
                playerAction.addUnitAction(u, attackAction);
                return;
            }
        }
        
        // Otherwise, move to find an enemy to attack
        if (closestEnemy != null)
        {
            UnitAction moveTowardsClosest = moveInRange(u, closestEnemy);
            if( moveTowardsClosest != null)
            {
                 playerAction.addUnitAction(u, moveTowardsClosest);
            }
        }

    }
    
    // Determine Ranged Unit Action (Miguel)
    // The logic for this type for now is defined as follows:
    // First check if an enemy unit is within the danger zone, ie. 1 space
    // If not, and there are enemies within range, attack
    // If no enemies in range, move until there is are enemies in range
    // Currently there is no logic for a defensive position set up. This is temporarily an aggressive ranged unit.
    // A flaw would be that it does not check if it is moving into more danger, it simply moves away from the first found nearest unit
    // As sophistication of this bot improves, more features and logic will be implemented
    // Currently, the effectiveness of this strategy is questionable because theoretically, a unit could simply be continuously pursued and would never attack.
    // In the larger sense of the game, however, due to the variable nature of the game, there are other factors at play - for example, perhaps heavy and light could assist in keeping space between enemy units and friendly ranged
    public void setRangedAction(Unit u) 
    {
        
        List<UnitAction> validActions = u.getUnitActions(gameState, 1);    // Gets all actions the unit can perform
        
        Unit closestEnemy = getClosestEnemy(u);
        int closestDistance = getSquareUnitDistance(u, closestEnemy);
        
        // If an enemy is within 1 space in any direction, move one space in a direction such that it is no longer 1 space away
        if (closestEnemy != null) 
        { // If there is an enemy
            // If in 'danger  zone', retreat
            if (closestDistance == 1) 
            {
                // Determine which direction to move away from danger
                int avoidX = closestEnemy.getX();
                int avoidY = closestEnemy.getY();
                int moveX, moveY;

                // Note: right now, it doesn't check if the move is valid until after, and if it isn't, it will move on to the next action check which is attack
                if (u.getX() < avoidX) { moveX = UnitAction.DIRECTION_LEFT; }
                else if (u.getX() > avoidX) { moveX = UnitAction.DIRECTION_RIGHT; }
                else { moveX = UnitAction.DIRECTION_NONE; }

                if (u.getX() < avoidY) { moveY = UnitAction.DIRECTION_DOWN; }
                else if (u.getX() > avoidY) { moveY = UnitAction.DIRECTION_UP; }
                else { moveY = UnitAction.DIRECTION_NONE; }
                
        UnitAction moveAction = new UnitAction(UnitAction.TYPE_MOVE, (int)Math.round((Math.random() * 3.0)));
                if (validActions.contains(moveAction) && gameState.isUnitActionAllowed(u, moveAction)) 
                {
                    playerAction.addUnitAction(u, moveAction);
                    return;
                }
            }
            
            // If the closest enemy is within range and is not within the danger zone specified above, and is within attack range, attack that unit
            // Currently, it is possible the unit is within the danger zone and this unit still carries out an attack action - to be refined as we develop a better strategy
            UnitAction attackAction = new UnitAction(UnitAction.TYPE_ATTACK_LOCATION, closestEnemy.getX(), closestEnemy.getY()); 
            if (validActions.contains(attackAction) && gameState.isUnitActionAllowed(u, attackAction)) 
            {
                playerAction.addUnitAction(u, attackAction);
                return;
            }
        }

        // Otherwise, move to find an enemy to attack
        if (closestEnemy != null)
        {
            UnitAction moveTowardsClosest = moveInRange(u, closestEnemy);
            if( moveTowardsClosest != null)
            {
                 playerAction.addUnitAction(u, moveTowardsClosest);
            }
        }
    }
    
    
    public void setBarracksAction(Unit u)
    {
        List<Unit> units = gameState.getUnits();
        int idleBarracks = 0;
        for(Unit currentUnit:units)
        {//finds how many idle barracks there are to consider the money that should be used
            if(currentUnit.isIdle(gameState) && currentUnit.getPlayer() == playerID && currentUnit.getType().name == "Barracks")
            {
                ++idleBarracks;
            }
        }
        int budget = (gameState.getPhysicalGameState().getPlayer(playerID).getResources() - playerAction.getResourceUsage().getResourcesUsed(playerID)) / idleBarracks;//the share of this barracks resources in evenly split amoung idle barracks. Ignores worker and base resource ussage.
        
        UnitType bestUnitType = gameState.getUnitTypeTable().getUnitTypes().get(0);//The best type of unit that can be produced
        int bestValue = Integer.MIN_VALUE;
        for(UnitType type:u.getType().produces)
        {
            if(type.cost <= budget)
            {//finds the best unit in budget or cheapest unit if none are within budget
                if(type.hp*type.maxDamage*type.attackRange/type.attackTime > bestValue)
                {
                    bestUnitType = type;
                    bestValue = bestUnitType.hp*bestUnitType.maxDamage*bestUnitType.attackRange/bestUnitType.attackTime;
                }
            }
            else
            {
                if(type.cost < bestUnitType.cost)
                {
                    bestUnitType = type;
                }
            }
        }
        
        bestUnitType = gameState.getUnitTypeTable().getUnitType((int)(4+(int)(Math.random()*3)));
        if(bestUnitType.cost > (gameState.getPhysicalGameState().getPlayer(playerID).getResources() - playerAction.getResourceUsage().getResourcesUsed(playerID)))
        {//if there are not enough resources to produce the unit returns
            return;
        }
        
        float bestDirectionWorth = -1;
        int bestDirection = 0;
        for(int i = 0; i < 4; ++i)
        {
            float  directionWorth = 0;
            int newX = u.getX() + UnitAction.DIRECTION_OFFSET_X[i];
            int newY = u.getY() + UnitAction.DIRECTION_OFFSET_Y[i];
            
            if(newX < 0 || newY < 0 ||  newX >= gameState.getPhysicalGameState().getWidth() || newY >= gameState.getPhysicalGameState().getHeight() || !gameState.free(newX, newY))
            {//skips spaces which are occupied or out of bounds
                continue;
            }
            
            for(Unit currentUnit:units)
            {//increases worth of unit being produced in a direction based on proximity to other units
                if(currentUnit.getPlayer() == playerID)
                {//bonus for ally proximity
                    int distance = 1 + Math.abs(newX - currentUnit.getX()) + Math.abs(newY - currentUnit.getY());
                   directionWorth += 1/distance ;//closer frendly units are more beneficial 
                }
                else
                {//bonus for enemy proximity
                   int distance = Math.abs(newX - currentUnit.getX()) + Math.abs(newY - currentUnit.getY());
                   directionWorth += 1/(1 + Math.abs(distance -bestUnitType.attackRange));//closer to the attack range is more valuable 
                }
            }
            
            if(directionWorth > bestDirectionWorth)
            {
                bestDirectionWorth = directionWorth;
                bestDirection = i;
            }
        }
        UnitAction bestAction = new UnitAction(UnitAction.TYPE_PRODUCE, bestDirection, bestUnitType);
        if(gameState.isUnitActionAllowed(u, bestAction))
        {
            playerAction.addUnitAction(u, bestAction);
        }
    }
    
    
    public void setBaseAction(Unit u)
    {
        List<UnitAction> validActions = u.getUnitActions(gameState, 1);//gets all actions the unit can perform
        if(gameState.isUnitActionAllowed(u, validActions.get(0)))
        {//makes sure the action does not conflict with other unit actions
            playerAction.addUnitAction(u, validActions.get(0));//does whatever happens to be at index 0. This should be altered to perform what is found to be the best action
        }
        
    }
    
    //returns the enemy closest to the unit u or null if there is none
    private Unit getClosestEnemy(Unit u){
        Unit closestEnemy = null;       // Checks for any enemy units nearby the heavy units
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
    
    //returns the distance squared between the units, this is the distance that would be considered towards attack range not the distance to travel to eachother
    //the square distance is used since square root is slow and range is dependant on the distance of a straight line between points rather than the spaces that would need traveled. 
    private int getSquareUnitDistance(Unit u, Unit other){
        return (other.getY() - u.getY())*(other.getY() - u.getY()) + (other.getX() - u.getX())*(other.getX() - u.getX());
    }
    
   //attempts to move within range to attach unit other, null will  be returned if no path exists
    private UnitAction moveInRange(Unit u, Unit other){
        int width = gameState.getPhysicalGameState().getWidth();
        return pathFinder.findPathToPositionInRange(u, other.getX() + other.getY()*width, u.getAttackRange(), gameState, playerAction.getResourceUsage());
    }
    
   //attempts to move adjacent to unit other, null will  be returned if no path exists
    private UnitAction moveTowardsUnit(Unit u, Unit other){
        int width = gameState.getPhysicalGameState().getWidth();
        return pathFinder.findPathToAdjacentPosition(u, other.getX() + other.getY()*width, gameState, playerAction.getResourceUsage());
    }
    
   //attempts to move towards the given position, null will  be returned if no path exists
    private UnitAction moveTowardsPositon(Unit u, int x, int y){
        int width = gameState.getPhysicalGameState().getWidth();
        return pathFinder.findPath(u, x + y*width, gameState, playerAction.getResourceUsage());
    }
    
    // This will be called by the microRTS GUI to get the

    // list of parameters that this bot wants exposed

    // in the GUI.

    public List<ParameterSpecification> getParameters()

    {
        return new ArrayList<>();
    }

}