package mins_bot.microrts-f23;

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

public class IliasAI extends AbstractionLayerAI 
{

    Random r = new Random();
    protected UnitTypeTable utt;
    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType heavyType;

    // Strategy:
    // If we have any "heavy": attack the nearest enemy unit
    // If we have a base: train worker until we have 3 worker
    // If we have a barracks: train heavy
    // If we have a worker: do this if needed: build base, build barracks, harvest resources

    /**
     * Constructor for IliasAI.
     * Initializes the AI with a UnitTypeTable and AStarPathFinding.
     *
     * @param a_utt The UnitTypeTable instance.
     */
    public IliasAI(UnitTypeTable a_utt) 
    {
        this(a_utt, new AStarPathFinding());
    }
    
    /**
     * Overloaded constructor for IliasAI.
     * Initializes the AI with a UnitTypeTable and custom PathFinding.
     *
     * @param a_utt The UnitTypeTable instance.
     * @param a_pf  The PathFinding instance.
     */
    public IliasAI(UnitTypeTable a_utt, PathFinding a_pf) 
    {
        super(a_pf);
        reset(a_utt);
    }

    /**
     * Resets the AI.
     */
    public void reset() 
    {
    	super.reset();
    }
    
    /**
     * Resets the AI with a new UnitTypeTable.
     *
     * @param a_utt The UnitTypeTable instance.
     */
    public void reset(UnitTypeTable a_utt)  
    {
        utt = a_utt;
        workerType = utt.getUnitType("Worker");
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");
        heavyType = utt.getUnitType("Heavy");
    }   
    
    /**
     * Clones the current AI instance.
     *
     * @return A cloned instance of IliasAI.
     */
    public AI clone() 
    {
        return new IliasAI(utt, pf);
    }

    /**
     * Gets the action to be performed by the AI in the current game state.
     *
     * @param player The player index.
     * @param gs     The current game state.
     * @return The PlayerAction to be executed.
     */
    public PlayerAction getAction(int player, GameState gs) 
    {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);
//        System.out.println("LightRushAI for player " + player + " (cycle " + gs.getTime() + ")");

        // behavior of bases:
        for (Unit u : pgs.getUnits()) 
        {
            if (u.getType() == baseType
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) 
            {
                baseBehavior(u, p, pgs);
            }
        }

        // behavior of barracks:
        for (Unit u : pgs.getUnits()) 
        {
            if (u.getType() == barracksType
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) 
            {
                barracksBehavior(u, p, pgs);
            }
        }

        // behavior of melee units:
        for (Unit u : pgs.getUnits()) 
        {
            if (u.getType().canAttack && !u.getType().canHarvest
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) 
            {
                meleeUnitBehavior(u, p, gs);
            }
        }

        // behavior of workers:
        List<Unit> workers = new LinkedList<>();
        for (Unit u : pgs.getUnits()) 
        {
            if (u.getType().canHarvest
                    && u.getPlayer() == player) 
            {
                workers.add(u);
            }
        }
        workersBehavior(workers, p, gs);

        // This method simply takes all the unit actions executed so far, and packages them into a PlayerAction
        return translateActions(player, gs);
    }

    /**
     * Resets the AI.
     */
    public void baseBehavior(Unit u, Player p, PhysicalGameState pgs) 
    {
        int nworkers = 0;
        for (Unit u2 : pgs.getUnits()) 
        {
            if (u2.getType() == workerType
                    && u2.getPlayer() == p.getID()) 
            {
                nworkers++;
            }
        }
        if (nworkers < 3 && p.getResources() >= workerType.cost)
        {
            train(u, workerType);
        }
    }

    /**
     * Defines the behavior for barracks.
     */
    public void barracksBehavior(Unit u, Player p, PhysicalGameState pgs) 
    {
        if (p.getResources() >= heavyType.cost) 
        {
            train(u, heavyType);
        }
    }

    /**
     * Defines the behavior for melee units.
     */
    public void meleeUnitBehavior(Unit u, Player p, GameState gs) 
    {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;
        for (Unit u2 : pgs.getUnits()) 
        {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) 
            {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestEnemy == null || d < closestDistance) 
                {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        }
        if (closestEnemy != null) 
        {
//            System.out.println("LightRushAI.meleeUnitBehavior: " + u + " attacks " + closestEnemy);
            attack(u, closestEnemy);
        }
    }

    /**
     * Defines the behavior for worker units.
     */
    public void workersBehavior(List<Unit> workers, Player p, GameState gs) 
    {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        int nbases = 0;
        int nbarracks = 0;

        int resourcesUsed = 0;
        List<Unit> freeWorkers = new LinkedList<>(workers);

        if (workers.isEmpty()) 
        {
            return;
        }

        for (Unit u2 : pgs.getUnits()) 
        {
            if (u2.getType() == baseType
                    && u2.getPlayer() == p.getID()) 
            {
                nbases++;
            }
            if (u2.getType() == barracksType
                    && u2.getPlayer() == p.getID()) 
            {
                nbarracks++;
            }
        }

        List<Integer> reservedPositions = new LinkedList<>();
        if (nbases == 0 && !freeWorkers.isEmpty()) 
        {
            // build a base:
            if (p.getResources() >= baseType.cost + resourcesUsed) 
            {
                Unit u = freeWorkers.remove(0);
                buildIfNotAlreadyBuilding(u,baseType,u.getX(),u.getY(),reservedPositions,p,pgs);
                resourcesUsed += baseType.cost;
            }
        }

        if (nbarracks == 0) 
        {
            // build a barracks:
            if (p.getResources() >= barracksType.cost + resourcesUsed && !freeWorkers.isEmpty()) 
            {
                Unit u = freeWorkers.remove(0);
                buildIfNotAlreadyBuilding(u,barracksType,u.getX(),u.getY(),reservedPositions,p,pgs);
                resourcesUsed += barracksType.cost;
            }
        }


        // harvest with all the free workers:
        List<Unit> stillFreeWorkers = new LinkedList<>();
        for (Unit u : freeWorkers) 
        {
            Unit closestBase = null;
            Unit closestResource = null;
            int closestDistance = 0;
            for (Unit u2 : pgs.getUnits()) 
            {
                if (u2.getType().isResource) 
                {
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestResource == null || d < closestDistance) 
                    {
                        closestResource = u2;
                        closestDistance = d;
                    }
                }
            }
            closestDistance = 0;
            for (Unit u2 : pgs.getUnits()) 
            {
                if (u2.getType().isStockpile && u2.getPlayer()==p.getID()) 
                {
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestBase == null || d < closestDistance) 
                    {
                        closestBase = u2;
                        closestDistance = d;
                    }
                }
            }
            boolean workerStillFree = true;
            if (u.getResources() > 0) 
            {
                if (closestBase!=null) 
                {
                    AbstractAction aa = getAbstractAction(u);
                    if (aa instanceof Harvest) 
                    {
                        Harvest h_aa = (Harvest)aa;
                        if (h_aa.getBase()!=closestBase) harvest(u, null, closestBase);
                    } 
                    else 
                    {
                        harvest(u, null, closestBase);
                    }
                    workerStillFree = false;
                }
            } 
            else 
            {            
                if (closestResource!=null && closestBase!=null) 
                {
                    AbstractAction aa = getAbstractAction(u);
                    if (aa instanceof Harvest) 
                    {
                        Harvest h_aa = (Harvest)aa;
                        if (h_aa.getTarget() != closestResource || h_aa.getBase()!=closestBase) harvest(u, closestResource, closestBase);
                    } 
                    else 
                    {
                        harvest(u, closestResource, closestBase);
                    }
                    workerStillFree = false;
                }
            }
            
            if (workerStillFree) stillFreeWorkers.add(u);            
        }
        
        for(Unit u:stillFreeWorkers) meleeUnitBehavior(u, p, gs);        
    }

    /**
     * Gets the parameters used by the AI.
     *
     * @return A list of ParameterSpecification objects.
     */
    @Override
    public List<ParameterSpecification> getParameters()
    {
        List<ParameterSpecification> parameters = new ArrayList<>();
        
        parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));

        return parameters;
    }    
    
}
