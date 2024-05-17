import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import rts.units.UnitTypeTable;

public class IliasAITest {
    private IliasAI IliasAI;
    private UnitTypeTable utt;

    @Before
    public void setUp() {
        utt = new UnitTypeTable();
        IliasAI = new IliasAI(utt);
    }

    @Test
    public void testInitialization() {
        assertNotNull("Worker unit should not be null", IliasAI.workerUnit);

    }

    @Test
    public void testBaseActions() {
        // mock GameState 
        PhysicalGameState pgs = new PhysicalGameState(8, 8); // assuming a 8x8 grid
        GameState gameState = new GameState(pgs, utt);
        Player player = new Player(0, 10); // Player ID 0 with 10 resources

        // Add a base for the player
        Unit base = new Unit(utt.getUnitType("Base"), player, 0, 0); // Assuming base at position (0,0)
        pgs.addUnit(base);

        // Set up game state
        gameState.addPlayer(player);
        gameState.setPhysicalGameState(pgs);

        // Call the AI's getAction method
        PlayerAction playerAction = IliasAI.getAction(0, gameState);

        // Check if the action to train a worker has been issued
        boolean workerTrained = false;
        for (Pair<Unit, AbstractAction> action : playerAction.getActions()) {
            if (action.m_b.getType() == utt.getUnitType("Worker")) {
                workerTrained = true;
                break;
            }
        }

        assertTrue("Base should train a worker", workerTrained);
    }

    @Test
    public void testWorkerActions() {
        // mock GameState 
        PhysicalGameState pgs = new PhysicalGameState(8, 8); // assuming a 8x8 grid
        GameState gameState = new GameState(pgs, utt);
        Player player = new Player(0, 10); // Player ID 0 with 10 resources

        // Add a base and a worker for the player
        Unit base = new Unit(utt.getUnitType("Base"), player, 0, 0); // Base at position (0,0)
        Unit worker = new Unit(utt.getUnitType("Worker"), player, 1, 0); // Worker at position (1,0)
        pgs.addUnit(base);
        pgs.addUnit(worker);

        // Add a resource
        Unit resource = new Unit(utt.getUnitType("Resource"), null, 3, 3); // Resource at position (3,3)
        pgs.addUnit(resource);

        // Set up game state
        gameState.addPlayer(player);
        gameState.setPhysicalGameState(pgs);

        // Call the AI's getAction method
        PlayerAction playerAction = IliasAI.getAction(0, gameState);

        // Check if the worker is performing a harvesting action
        boolean isHarvesting = false;
        for (Pair<Unit, AbstractAction> action : playerAction.getActions()) {
            if (action.m_a instanceof Harvest && action.m_b == worker) {
                isHarvesting = true;
                break;
            }
        }

        assertTrue("Worker should perform harvesting", isHarvesting);
    }
}
    


