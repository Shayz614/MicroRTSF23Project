package mins_bot;
      
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import java.util.ArrayList;
import java.util.List;
import rts.GameState;
import rts.PlayerAction;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction2;
import rts.UnitActionAssignment;


class statePendingActions
{
    public PlayerAction playerAction;
    boolean freeSpaces[][];
    int availiableResources;
    int playerID;
    GameState gameState;
    ArrayList <Unit> idleUnits;
    int numberOfIdleUnits;
    int numberOfWorkerUnits;
    int numberOfBaseUnits;
    int numberOfBarracksUnits;
    int numberOfLightUnits;
    int numberOfHeavyUnits;
    int numberOfRangedUnits;

    public statePendingActions(GameState gs, int player)
    {
        playerID = player;
        gameState = gs;
        playerAction = new PlayerAction();
        freeSpaces = gs.getAllFree();
        availiableResources = gameState.getPlayer(player).getResources() - gameState.getResourceUsage().getResourcesUsed(playerID);

        numberOfWorkerUnits = 0;
        numberOfBaseUnits = 0;
        numberOfBarracksUnits = 0;
        numberOfLightUnits = 0;
        numberOfHeavyUnits = 0;
        numberOfRangedUnits = 0;
        numberOfIdleUnits = 0;
        idleUnits = new ArrayList<>();
        for(Unit u : gameState.getUnits())
        {//goes through each unit and calls the relavent function to set its action if needed
            if(u.getPlayer() == player)
            {
                if(u.isIdle(gameState))
                {
                    idleUnits.add(u);
                    ++numberOfIdleUnits;
                }

                switch(u.getType().name){
                    case "Worker" -> ++numberOfWorkerUnits;
                    case "Light" -> ++numberOfLightUnits;
                    case "Heavy" -> ++numberOfHeavyUnits;
                    case "Ranged" -> ++numberOfRangedUnits;
                    case "Barracks" -> ++numberOfBarracksUnits;
                    case "Base" -> ++numberOfBaseUnits;
                    default -> {
                    }
                }
            }
        }
    }

    public void addUnitAction(Unit u, UnitAction action)
    {
        int x , y;
        switch(action.getType()){
            case UnitAction.TYPE_ATTACK_LOCATION -> {
            }
            case UnitAction.TYPE_HARVEST -> {
            }
            case UnitAction.TYPE_MOVE -> {
                //set the old position to be free and the new as occupied
                x = u.getX() + UnitAction.DIRECTION_OFFSET_X[action.getDirection()];
                y = u.getY() + UnitAction.DIRECTION_OFFSET_Y[action.getDirection()];
                freeSpaces[x][y] = false;
                freeSpaces[u.getX()][u.getY()] = true;
            }
            case UnitAction.TYPE_PRODUCE -> {
                //sets the new units spot as occupied and lowers the availiable resources
                x = u.getX() + UnitAction.DIRECTION_OFFSET_X[action.getDirection()];
                y = u.getY() + UnitAction.DIRECTION_OFFSET_Y[action.getDirection()];
                freeSpaces[x][y] = false;
                availiableResources -= action.getUnitType().cost;
            }
            default -> {
            }
        }

        playerAction.addUnitAction(u, action);
        idleUnits.remove(u);
        numberOfIdleUnits--;
    }
    public boolean canAddUnitAction(Unit u, UnitAction action)
    {//asumes the unit is idle and action is a possible action
        int x , y;
        switch(action.getType()){
            case UnitAction.TYPE_ATTACK_LOCATION -> {
                if(!isInBounds(action.getLocationX(), action.getLocationY()))return false;
                int xDist = action.getLocationX() - u.getX();
                int yDist = action.getLocationY() - u.getY();
                if(xDist*xDist + yDist*yDist > u.getAttackRange()*u.getAttackRange())return false;
            }
            case UnitAction.TYPE_HARVEST -> {
                x = u.getX() + UnitAction.DIRECTION_OFFSET_X[action.getDirection()];
                y = u.getY() + UnitAction.DIRECTION_OFFSET_Y[action.getDirection()];
                
                if(!isInBounds(x, y) || gameState.getPhysicalGameState().getUnitAt(x,y) == null || !gameState.getPhysicalGameState().getUnitAt(x,y).getType().isResource)
                {
                    return false;
                }
            }
            case UnitAction.TYPE_RETURN -> {
                x = u.getX() + UnitAction.DIRECTION_OFFSET_X[action.getDirection()];
                y = u.getY() + UnitAction.DIRECTION_OFFSET_Y[action.getDirection()];
                
                if(!isInBounds(x, y) || gameState.getPhysicalGameState().getUnitAt(x,y) == null || !gameState.getPhysicalGameState().getUnitAt(x,y).getType().isStockpile)
                {
                    return false;
                }
            }
            case UnitAction.TYPE_MOVE -> {
                //set the old position to be free and the new as occupied
                x = u.getX() + UnitAction.DIRECTION_OFFSET_X[action.getDirection()];
                y = u.getY() + UnitAction.DIRECTION_OFFSET_Y[action.getDirection()];
                if(!isInBounds(x, y))
                {
                    return false;
                }
                if(freeSpaces[x][y] == false && !willBecomeFree(x, y, u.getMoveTime())){
                    return false;
                }
            }
            case UnitAction.TYPE_PRODUCE -> {
                //sets the new units spot as occupied and lowers the availiable resources
                x = u.getX() + UnitAction.DIRECTION_OFFSET_X[action.getDirection()];
                y = u.getY() + UnitAction.DIRECTION_OFFSET_Y[action.getDirection()];
                if(!isInBounds(x, y) || freeSpaces[x][y] == false)
                {
                    return false;
                }
                if(availiableResources < action.getUnitType().cost)
                {
                    return false;
                }
            }
            default -> {
            }
        }

        return true;
    }
    public boolean willBecomeFree(int x, int y, int time){
        Unit unit = gameState.getPhysicalGameState().getUnitAt(x, y);
        if(unit == null)
        {
            return false;
        }
        
        UnitActionAssignment actionAssignment= gameState.getActionAssignment(gameState.getPhysicalGameState().getUnitAt(x, y));
        if(actionAssignment == null)
        {
            return false;
        }
        
        if(gameState.getActionAssignment(gameState.getPhysicalGameState().getUnitAt(x, y)).action.getType() != UnitAction.TYPE_MOVE)
        {
            return false;
        }
        
        if(gameState.getActionAssignment(gameState.getPhysicalGameState().getUnitAt(x, y)).time > time)
        {
            return false;
        }
        
        return true;
    }
    
    public boolean isInBounds(int x, int y){
       return !(x < 0 || y < 0 ||  x >= gameState.getPhysicalGameState().getWidth() || y >= gameState.getPhysicalGameState().getHeight());
    }

    private boolean unitIsInbounds(Unit u){
        return isInBounds(u.getX(), u.getY());
    }
}




public class MINS_Bot extends AIWithComputationBudget 
{
    UnitTypeTable m_utt = null;
    private long startTime; //Variable to track when the decision-making process starts
    public long maxTime;  //Maximum allowed time for computation
    GameState gameState;
    int playerID;
    AStarPathFinding pathFinder;
    boolean isSimulation;
    boolean outputAdditionalInfo;
    boolean outputBasicInfo;
    EvaluationFunction ef ;
    final int NONE_ACTION_DURATION = 1;
    

    
    statePendingActions pendingState;
    
    // This is the default constructor that microRTS will call:
    public MINS_Bot(UnitTypeTable utt) 
    {
        super(-1,-1);
        
        pathFinder = new AStarPathFinding();
        m_utt = utt;
        setTimeBudget(TIME_BUDGET);
        setEvaluationFunction(new SimpleSqrtEvaluationFunction2());
        isSimulation = false;
        //ef = new SimpleSqrtEvaluationFunction3();
        //ef = new SimpleSqrtEvaluationFunction2();
        //ef = new LanchesterEvaluationFunction();
        outputAdditionalInfo = false;
        outputBasicInfo = false;
    }

    @Override
    public AI clone() 
    {
        return new MINS_Bot(m_utt);
    }

    // This will be called once at the beginning of each new game:    
    @Override
    public void reset() 
    {

    }

    // Called by microRTS at each game cycle.

    // Returns the action the bot wants to execute.

    @Override
    public PlayerAction getAction(int player, GameState gs) 
    {
        gameState = gs;
        playerID = player;
        pendingState = new statePendingActions(gs, player);
        
        startTime = System.nanoTime();
        while(System.nanoTime()-startTime < maxTime &&!pendingState.idleUnits.isEmpty()){
            Unit u = pendingState.idleUnits.get(0);
            switch(u.getType().name){
                case "Worker" -> setWorkerAction(u);
                case "Light" -> setLightAction(u);
                case "Heavy" -> setHeavyAction(u);
                case "Ranged" -> setRangedAction(u);
                case "Barracks" -> setBarracksAction(u);
                case "Base" -> setBaseAction(u);
                default -> {
                }
            }
            if(pendingState.idleUnits.contains(u)){//if the unit did not decide an action to execute make it do a none action
                pendingState.addUnitAction(u, new UnitAction(UnitAction.TYPE_NONE, NONE_ACTION_DURATION));
            }
        }


        
        //fills any remaining actions with a longer none action this should only happen if time ran out
        pendingState.playerAction.fillWithNones(gameState, player, NONE_ACTION_DURATION*2);
                
        return pendingState.playerAction;
    }    
    
    //function for each unit type to decide the action the given unit should take and add it to playerActions
    public void setWorkerAction(Unit u)
    {
        
        ArrayList<UnitAction> validActions = new ArrayList<>();
        validActions.add( new UnitAction(UnitAction.TYPE_NONE, 1));
        UnitAction potentialAction;
        for(int i = 0; i < 4; ++i)
        {//adds actions that should be considered in each direction
            //adds harvesting
            potentialAction = new UnitAction(UnitAction.TYPE_HARVEST, i);
            if(pendingState.canAddUnitAction(u, potentialAction)){
                //if(gameState.getPhysigameStatecalGameState().getUnitAt(u.getX() + UnitAction.DIRECTION_OFFSET_X[i], u.getY() + UnitAction.DIRECTION_OFFSET_Y[i]).getType().isResource)
                    validActions.add(potentialAction);
            }
            //adds returning
            potentialAction = new UnitAction(UnitAction.TYPE_RETURN, i);
            if(pendingState.canAddUnitAction(u, potentialAction)){
               validActions.add(potentialAction);
            }
            //adds base production
            potentialAction = new UnitAction(UnitAction.TYPE_PRODUCE, i, m_utt.getUnitType("Base"));
            if(pendingState.canAddUnitAction(u, potentialAction)){
               validActions.add(potentialAction);
            }
            //adds barracks production
            potentialAction = new UnitAction(UnitAction.TYPE_PRODUCE, i, m_utt.getUnitType("Barracks"));
            if(pendingState.canAddUnitAction(u, potentialAction)){
               validActions.add(potentialAction);
            }
            //adds attack
            potentialAction = new UnitAction(UnitAction.TYPE_ATTACK_LOCATION, u.getX() + UnitAction.DIRECTION_OFFSET_X[i], u.getY() + UnitAction.DIRECTION_OFFSET_Y[i]); 
            if(pendingState.canAddUnitAction(u, potentialAction)){
               validActions.add(potentialAction);
            }
        }      
        
        ArrayList<String> targets = new ArrayList<>();//types to target and move towards
        if(u.getResources() == 0)targets.add("Resource");//adds resource if it is not carrying resources
        if(u.getResources() != 0)targets.add("Base");//Base if it is  carrying resources
        
        ArrayList<Unit> targetUnits = new ArrayList<>();//the closest a reachable units of target types
        for(String target: targets)
        {//adds the units if they exist
            Unit targetUnit = getClosestReachableOfType(u, m_utt.getUnitType(target));
            if(targetUnit != null){
                targetUnits.add(targetUnit);
            }
            
        }
        
        if(targetUnits.isEmpty())
        {//if there were no valid targets attempts to move towards an enemy
            Unit targetUnit = getClosestReachableEnemy(u);//targets the clostest enemy 
            if(targetUnit != null){
                targetUnits.add(targetUnit);
            }else{
                for(int i = 0; i < 4; ++i)
                {//if there were still no valid targets adds all legal directions to move
                    UnitAction move = new UnitAction(UnitAction.TYPE_MOVE, i);
                    if(pendingState.canAddUnitAction(u, move) && !validActions.contains(move))
                    {//adds the action to move towards the target if possible and the same action is not already in the valid actions
                        validActions.add(move);
                    }
                }
            }
        }
        
        for (Unit target : targetUnits)
        {//adds actions to move adjacent to the target if possible
            
            UnitAction moveTowardsTarget = moveTowardsUnit(u, target);
            if(moveTowardsTarget != null)
            {
                if(pendingState.canAddUnitAction(u, moveTowardsTarget) && !validActions.contains(moveTowardsTarget))
                {//adds the action to move towards the target if possible and the same action is not already in the valid actions
                    validActions.add(moveTowardsTarget);
                }
            }
        }
        
        UnitAction bestAction = getBestSimulatedAction(u, validActions);
        if(bestAction != null && pendingState.canAddUnitAction(u, bestAction))
        {
            pendingState.addUnitAction(u, bestAction);
        }
        
    }
    
    
    public void setLightAction(Unit u)
    {
        Unit closestEnemy = getClosestEnemy(u);
        
         if (closestEnemy == null)
         {//if there are no enemy units found do nothing
             return;
         }
         
        // Prioritize attacking closest enemy first        
        UnitAction attackAction = new UnitAction(UnitAction.TYPE_ATTACK_LOCATION, closestEnemy.getX(), closestEnemy.getY()); 
        if(pendingState.canAddUnitAction(u, attackAction)) 
        {
            pendingState.addUnitAction(u, attackAction);
            return;
        }
        
        // Otherwise, move towards the closest enemy
        UnitAction moveTowardsClosest = moveInRange(u, closestEnemy);
        if(moveTowardsClosest != null)
        {
             pendingState.addUnitAction(u, moveTowardsClosest);
        }
    }
    
    
    public void setHeavyAction(Unit u)
    {
        Unit closestEnemy = getClosestEnemy(u);
        
         if (closestEnemy == null)
         {//if there are no enemy units found do nothing
             return;
         }
         
        // Prioritize attacking closest enemy first        
        UnitAction attackAction = new UnitAction(UnitAction.TYPE_ATTACK_LOCATION, closestEnemy.getX(), closestEnemy.getY()); 
        if(pendingState.canAddUnitAction(u, attackAction)) 
        {
            pendingState.addUnitAction(u, attackAction);
            return;
        }
        
        // Otherwise, move towards the closest enemy
        UnitAction moveTowardsClosest = moveInRange(u, closestEnemy);
        if(moveTowardsClosest != null)
        {
             pendingState.addUnitAction(u, moveTowardsClosest);
        }
    }
    
    
    public void setRangedAction(Unit u) 
    {
        Unit closestEnemy = getClosestEnemy(u);
        int closestDistance = getSquareUnitDistance(u, closestEnemy);
        
        
        if (closestEnemy != null) 
        {//if it it is close to a unit that can attack and has a lower attack range attempt to move away before attacking
           if (closestEnemy.getType().canAttack && closestEnemy.getAttackRange() < u.getAttackRange() && closestDistance <= (1+(closestEnemy.getAttackRange())*(closestEnemy.getAttackRange()))*2) 
           {
                // Determine which direction to move away from danger
                ArrayList<UnitAction> retreatActions = new ArrayList<>();
                int y = u.getY();
                int x = u.getX();
                Unit potentialUnit = new Unit(u);
                for(int i = 0; i < 4; ++i)
                {//adds each direction to be considered if it puts it farther from the closest unit
                    UnitAction retreatAction = new UnitAction(UnitAction.TYPE_MOVE, i);
                    if(!pendingState.canAddUnitAction(u, retreatAction))continue;//skip if the action would not be valid
                    int newX = x+UnitAction.DIRECTION_OFFSET_X[i], newY = y+UnitAction.DIRECTION_OFFSET_Y[i];
                    potentialUnit.setX(newX);
                    potentialUnit.setY(newY);
                    if(getSquareUnitDistance(potentialUnit, getClosestEnemy(potentialUnit)) > closestDistance)
                    {
                        retreatActions.add(retreatAction);
                    }
                }
                
                if (!retreatActions.isEmpty())//if one or more retreat actions were found get the best simulated one
                {
                    UnitAction moveAction = getBestSimulatedAction(u, retreatActions);
                    if(pendingState.canAddUnitAction(u, moveAction))
                    {
                        pendingState.addUnitAction(u, moveAction);
                        return;
                    }
                }
            }
            
            //if it does not retreat attack the closest enemy if possible
            UnitAction attackAction = new UnitAction(UnitAction.TYPE_ATTACK_LOCATION, closestEnemy.getX(), closestEnemy.getY()); 
            if (pendingState.canAddUnitAction(u, attackAction)) 
            {
                pendingState.addUnitAction(u, attackAction);
                return;
            }
            
            //if the closest enemy will be moving into range either attack its future spot or wait (preventing moving too close when attempting to get in range)
            if(gameState.getUnitAction(closestEnemy) != null && gameState.getUnitAction(closestEnemy).getType() == UnitAction.TYPE_MOVE)
            {
                int direction = gameState.getUnitAction(closestEnemy).getDirection();
                Unit potentialEnemyUnit = new Unit(closestEnemy);
                potentialEnemyUnit.setX(potentialEnemyUnit.getX() + UnitAction.DIRECTION_OFFSET_X[direction]);
                potentialEnemyUnit.setY(potentialEnemyUnit.getY() + UnitAction.DIRECTION_OFFSET_Y[direction]);
                if(getSquareUnitDistance(u, potentialEnemyUnit) <= u.getAttackRange() * u.getAttackRange())
                {
                    if(gameState.getActionAssignment(closestEnemy).time <= u.getAttackTime())
                    {
                        attackAction = new UnitAction(UnitAction.TYPE_ATTACK_LOCATION, potentialEnemyUnit.getX(), potentialEnemyUnit.getY()); 
                        if (pendingState.canAddUnitAction(u, attackAction)) 
                        {
                            pendingState.addUnitAction(u, attackAction);
                            return;
                        }
                    }
                    else
                    {
                        return;
                    }
                }
            }
        }
        
        
        // Otherwise, move towards the closest reachable enemy to attack if possible
        Unit closestReachableEnemy = getClosestReachableEnemy(u);
        if (closestReachableEnemy != null)
        {
            UnitAction moveTowardsClosest = moveInRange(u, closestReachableEnemy);
            if(moveTowardsClosest != null && pendingState.canAddUnitAction(u, moveTowardsClosest))
            {
                 pendingState.addUnitAction(u, moveTowardsClosest);
            }
        }
    }
    
    public void setBarracksAction(Unit u)
    {
        setSimulatedAction(u);
    }
    
    public void setBaseAction(Unit u)
    {
        if(pendingState.numberOfWorkerUnits < pendingState.numberOfBaseUnits*4)//limits the max amount of workers
        {
            setSimulatedAction(u);
        }
    }
    
    private void setSimulatedAction(Unit u){//gets the best aciton from simulating all possible actions
        List<UnitAction> validActions = u.getUnitActions(gameState, NONE_ACTION_DURATION);    // Gets all actions the unit can perform
        if(validActions.isEmpty())//returns when the unit cannot perform any actions
        {
            return;
        }
       
        for(int i=0; i < validActions.size(); ++i)
        {//removes invalid actions
            UnitAction action = validActions.get(i);
            if(!pendingState.canAddUnitAction(u, action)){
                validActions.remove(i);
                --i;
            }
        }
        
        UnitAction bestAction = getBestSimulatedAction(u, validActions);
        if(bestAction != null && pendingState.canAddUnitAction(u, bestAction))
        {
            pendingState.addUnitAction(u, bestAction);
        }
    }
    
    private UnitAction getBestSimulatedAction(Unit u, List<UnitAction> actions)
    {//simulates the passed acitons and returns the best or null if there is no action
        if(actions.isEmpty())
        {
            return null;
        }
        
        UnitAction bestAction = null;
        double bestScore = -Double.MAX_VALUE;
        long actionEvalTime = (long)((float)(maxTime-(System.nanoTime()-startTime))/(float)pendingState.numberOfIdleUnits/(float)actions.size());//the eval time is the total time remaing split between each idle unit and each action this unit can perform
         for(UnitAction action:actions)
         {
            PlayerAction simulatedAction = new PlayerAction();
            simulatedAction.addUnitAction(u, action);
            GameState simulatedGameState = gameState.cloneIssue(simulatedAction);
            double score = simulate(simulatedGameState, actionEvalTime);
            if(outputAdditionalInfo)System.out.println("Unit, potential Action, score: " + u + ", " + simulatedAction+ ", " + score);
            if(score > bestScore){
                bestAction = action;
                bestScore = score;
            }
                
        }
        
         return bestAction;
    }
    
    public double simulate(GameState gs, long time) 
    {
      try{
        ArrayList<MultithreadedSimulate> simulations = new ArrayList<>();
        AI[]  simulatedAIs= new AI[]{new ai.abstraction.EconomyMilitaryRush(m_utt), new ai.abstraction.EconomyRush(m_utt), new ai.abstraction.WorkerRush(m_utt),/* new ai.abstraction.cRush.CRush_V2(m_utt),*/ new ai.RandomAI(m_utt)};
        if(outputAdditionalInfo)System.out.println("time: " + time);
        final int SKIP_CYCLES = 2;//the number of cycles to skip (fill with none actions) in order to allow further look ahead at the cost of simulation accuracy
        double score = 0;
        long startTime = System.nanoTime();
        
        for(AI a: simulatedAIs){
            MultithreadedSimulate simulation = new MultithreadedSimulate();
            simulation.setupSimulation(a, a, playerID,  gs, time, ef,  startTime);
            simulations.add(simulation);
        }
        
        for(int i = 0; i < simulations.size(); ++i){
            simulations.get(i).start();
        }
        
        for(int i = 0; i < simulations.size(); ++i){
            simulations.get(i).join();
            score += simulations.get(i).getScore();
        }
        
        return score;
     }catch (Exception e) {
         //e.printStackTrace();
      }
      return Double.NEGATIVE_INFINITY;
    }
    
    
    public double simulate_singlethread(GameState gs, long time) 
    {
      try{
        ArrayList<GameState> simulatedGameStates = new ArrayList<>();
        AI[]  simulatedAIs= new AI[]{new ai.abstraction.EconomyMilitaryRush(m_utt, pathFinder), new ai.abstraction.EconomyRush(m_utt, pathFinder), new ai.abstraction.WorkerRush(m_utt, pathFinder), /*new ai.abstraction.cRush.CRush_V2(m_utt), */new ai.RandomAI(m_utt)};
        if(outputAdditionalInfo)System.out.println("time: " + time);
        final int SKIP_CYCLES = 2;//the number of cycles to skip (fill with none actions) in order to allow further look ahead at the cost of simulation accuracy
        double score = 0;
        long startTime = System.nanoTime();
        int cycles = 0;
        int skippedCycles = 0;
        for(AI a: simulatedAIs){
            simulatedGameStates.add(gs.clone());
        }
        
        do{
                 for(int i = 0; i < simulatedGameStates.size(); ++i){
                    simulatedGameStates.get(i).issue(simulatedAIs[i].getAction(0, simulatedGameStates.get(i)));
                    simulatedGameStates.get(i).issue(simulatedAIs[i].getAction(1, simulatedGameStates.get(i)));
                    simulatedGameStates.get(i).cycle();
                 }
                ++cycles;
                
                double evaluation = 0;
                for(GameState g:simulatedGameStates){
                    evaluation += ef.evaluate(playerID, 1-playerID, g);
                }
                score += evaluation*Math.pow(0.99,cycles/10.0);
                for(int i = 0; i < SKIP_CYCLES; ++i){//skips cycles in order to allow further look ahead at the cost of simulation accuracy
                    for(GameState g:simulatedGameStates){
                        g.cycle();
                    }
                        skippedCycles++;
                }             
         }while(((System.nanoTime()-startTime) < time));
        if(outputAdditionalInfo)
        {
            System.out.println("\nSimulate Cycles: "+ cycles);
            System.out.println("Skipped Cycles: "+ skippedCycles);
        }else{
            if(!isSimulation && outputBasicInfo)System.out.println("Simulate Cycles: "+ cycles);
        }

        double evaluation = 0;
        for(GameState g:simulatedGameStates){//evaluates pending actiona
            g.forceExecuteAllActions();
            evaluation += ef.evaluate(playerID, 1-playerID, g);
        }
        score += evaluation*Math.pow(0.99,(cycles+1)/10.0)/2; //only give the pending actions partial weight

        return score;
     }catch (Exception e) {
         //e.printStackTrace();
      }
      return Double.NEGATIVE_INFINITY;
    }
    
     
    
    //returns the closest unit of type to the unit u that can be reached or null if there is none
    private Unit getClosestReachableOfType(Unit u, UnitType type)
    {
        Unit closest = null;       // Checks for any enemy units nearby the heavy units
        int closestDistance = Integer.MAX_VALUE;
        
        for (Unit unit : gameState.getUnits()) 
        {
            if (unit.getType() == type) 
            { 
                int distance = distanceToMoveInRange(u, unit);
                if (distance != -1 && distance < closestDistance) 
                {
                    closestDistance = distance;
                    closest= unit;
                }
            }
        }
        return closest;
    }
    
    //returns the closest unit of type to the unit u or null if there is none
    private Unit getClosestOfType(Unit u, UnitType type)
    {
        Unit closest = null;       // Checks for any enemy units nearby the heavy units
        int closestDistance = Integer.MAX_VALUE;
        
        for (Unit unit : gameState.getUnits()) 
        {
            if (unit.getType() == type) 
            { 
                int distance = getSquareUnitDistance(u, unit);
                if (distance < closestDistance) 
                {
                    closestDistance = distance;
                    closest= unit;
                }
            }
        }
        return closest;
    }
    
    //returns the enemy closest to the unit u or null if there is none
    private Unit getClosestEnemy(Unit u)
    {
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
    
    private Unit getClosestReachableEnemy(Unit u)
    {
        Unit closestEnemy = null;       // Checks for any enemy units nearby the heavy units
        int closestDistance = Integer.MAX_VALUE;
        for (Unit unit : gameState.getUnits()) 
        {
            if (unit.getPlayer() >= 0 && unit.getPlayer() != u.getPlayer()) 
            { 
                int distance = distanceToMoveInRange(u, unit);
                if (distance != -1 && distance < closestDistance) 
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
    private int getSquareUnitDistance(Unit u, Unit other)
    {
        return (other.getY() - u.getY())*(other.getY() - u.getY()) + (other.getX() - u.getX())*(other.getX() - u.getX());
    }
    
    //finds the distance to move within a specified range or -1 if unreachable
    private int distanceToMoveInRange(Unit u, Unit other, int range)
    {
        int width = gameState.getPhysicalGameState().getWidth();
        return pathFinder.findDistToPositionInRange(u, other.getX() + other.getY()*width, range, gameState, pendingState.playerAction.getResourceUsage());
    }
    
   //finds the distance to move within range or -1 if unreachable
    private int distanceToMoveInRange(Unit u, Unit other)
    {
        return distanceToMoveInRange(u, other,  u.getAttackRange());
    }
    
   //attempts to move within range to attach unit other, null will  be returned if no path exists
    private UnitAction moveInRange(Unit u, Unit other, int range)
    {
        int width = gameState.getPhysicalGameState().getWidth();
        return pathFinder.findPathToPositionInRange(u, other.getX() + other.getY()*width, range, gameState, pendingState.playerAction.getResourceUsage());
    }
    
   //attempts to move within range to attach unit other, null will  be returned if no path exists
    private UnitAction moveInRange(Unit u, Unit other)
    {
        return moveInRange(u, other, u.getAttackRange());
    }
    
   //attempts to move towards the given position, null will  be returned if no path exists
    private UnitAction moveTowardsPositon(Unit u, int x, int y)
    {
        int width = gameState.getPhysicalGameState().getWidth();
        return pathFinder.findPath(u, x + y*width, gameState, pendingState.playerAction.getResourceUsage());
    }
    
   //attempts to move adjacent to unit other, null will  be returned if no path exists
    private UnitAction moveTowardsUnit(Unit u, Unit other)
    {
        int width = gameState.getPhysicalGameState().getWidth();
        return pathFinder.findPathToAdjacentPosition(u, other.getX() + other.getY()*width, gameState, pendingState.playerAction.getResourceUsage());
    }
    
    
    // This will be called by the microRTS GUI to get the
    // list of parameters that this bot wants exposed
    // in the GUI.
    public List<ParameterSpecification> getParameters()
    {
        List<ParameterSpecification> parameters = new ArrayList<>();
        
        parameters.add(new ParameterSpecification("TimeBudget",int.class,60));
        parameters.add(new ParameterSpecification("EvaluationFunction", EvaluationFunction.class, new SimpleSqrtEvaluationFunction2()));
        
        return parameters;
    }
    
    public void setEvaluationFunction(EvaluationFunction evalFunc) {
           ef = evalFunc;
    }  
    
    public void setTimeBudget(int milisseconds) {
        TIME_BUDGET = milisseconds;
        maxTime = TIME_BUDGET*1000000; //Sets the maxTime to TIME_BUDGET in nanoseconds
        maxTime = (long)(maxTime*.9);//only tries to use part of the max time to avoid going over
    }
    
    public void preGameAnalysis(GameState g, long i) {
    }

}


class MultithreadedSimulate extends Thread  
{
    AI ai1;
    AI ai2;
    int playerID;
    GameState simulatedGameState;
    long time;
    EvaluationFunction ef;
    long startTime;
    double  score;
    
    public void setupSimulation(AI a1, AI a2, int id, GameState gs, long t, EvaluationFunction e, long start){
        ai1 = a1.clone();
        ai2 = a2.clone();
        playerID = id;
        simulatedGameState = gs.clone();
        time = t;
        ef = e;
        startTime = start;
    }
    
    public void run(){
        simulate();
    }
    
    public double getScore(){
        return score;
    }
    
     public void simulate() 
    {
         int cycles = 0;
      try{
        final int SKIP_CYCLES = 2;//the number of cycles to skip (fill with none actions) in order to allow further look ahead at the cost of simulation accuracy
        int skippedCycles = 0;
        
        score = 0;
        do{
               simulatedGameState.issue(ai1.getAction(0, simulatedGameState));
                simulatedGameState.issue(ai2.getAction(1, simulatedGameState));
                simulatedGameState.cycle();
                ++cycles;
                
                double evaluation = ef.evaluate(playerID, 1-playerID, simulatedGameState);
                score += evaluation*Math.pow(0.95,cycles);
                for(int i = 0; i < SKIP_CYCLES; ++i){//skips cycles in order to allow further look ahead at the cost of simulation accuracy
                    simulatedGameState.cycle();
                    skippedCycles++;
                }             
         }while(((System.nanoTime()-startTime) < time));
        

        simulatedGameState.forceExecuteAllActions();
        double evaluation = ef.evaluate(playerID, 1-playerID, simulatedGameState);
        score += evaluation*Math.pow(0.95,(cycles+1))/2; //only give the pending actions partial weight

         //System.out.println(cycles);
     }catch (Exception e) {
         e.printStackTrace();
      }
    }
}
