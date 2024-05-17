/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */
package Miguel_MINS_Bot;

import static junit.framework.Assert.assertEquals;
import org.junit.Test;
import org.junit.*;

// microRTS imports for testing
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.*;
import rts.UnitAction;
import ai.core.AI;
import ai.*;
import miguel_mins_bot.Miguel_MINS_Bot;
/**
 *
 * @author Miguel
 */
public class Miguel_MINS_BotTest {
    
    public Miguel_MINS_BotTest() {
    }
    
    /**
     * Test of posFree method, of class Miguel_MINS_Bot.
     */
    @Test
    public void testPosFree() throws Exception { 
        UnitTypeTable utt = new UnitTypeTable();
        PhysicalGameState pgs = PhysicalGameState.load("maps/16x16/basesWorkers16x16.xml", utt);

        GameState gs = new GameState(pgs, utt);
        
        Miguel_MINS_Bot ai = new Miguel_MINS_Bot(utt); 
        
        ai.getAction(0, gs); // initialize stuff like the game state
        
        // Test middle of the map - should be empty
        boolean expected = true;
        boolean actual =  ai.posFree(8,8,UnitAction.DIRECTION_NONE);
        assertEquals(expected, actual);
        
        
        // Test a taken position where the resources are
        expected = false;
        actual = ai.posFree(pgs.getWidth()-1, pgs.getHeight()-1, -1);
        assertEquals(expected, actual);
    }

    /**
     * Test of getClosestEnemyType method, of class Miguel_MINS_Bot.
     */
    @Test
    public void testGetClosestEnemyType() throws Exception {
        UnitTypeTable utt = new UnitTypeTable();
        PhysicalGameState pgs = PhysicalGameState.load("maps/16x16/basesWorkers16x16.xml", utt);

        GameState gs = new GameState(pgs, utt);
        
        AI ai1 = new PassiveAI(utt);
        Miguel_MINS_Bot ai2 = new Miguel_MINS_Bot(utt); 
        
        // Test middle of the map - should be empty
        Unit expected = gs.getPhysicalGameState().getUnitAt(14,14);
        Unit actual =  ai2.getClosestEnemyType(gs.getPhysicalGameState().getUnitAt(14, 14), "base");
        assertEquals(expected, actual); // Should return itself
    }

    /**
     * Test of isInRange method, of class Miguel_MINS_Bot.
     */
    @Test
    public void testIsInRange() throws Exception {
        UnitTypeTable utt = new UnitTypeTable();
        PhysicalGameState pgs = PhysicalGameState.load("maps/16x16/basesWorkers16x16.xml", utt);

        GameState gs = new GameState(pgs, utt);
        
        Miguel_MINS_Bot ai = new Miguel_MINS_Bot(utt);
        
        ai.getAction(0, gs);
        
        // Create a worker right next to the base
        UnitType worker = utt.getUnitType("Worker");
        Unit u = new Unit(0, worker, 13, 14);
        gs.getPhysicalGameState().addUnit(u);
        
        // Check the newly created unit is in range of base, should be true
        boolean expected = true;
        boolean actual = ai.isInRange(u, gs.getPhysicalGameState().getUnitAt(13,13));
        assertEquals(expected, actual);
        
        // Check the default worker is NOT in range of base, should return false
        expected = false;
        actual = ai.isInRange(gs.getPhysicalGameState().getUnitAt(14,14), gs.getPhysicalGameState().getUnitAt(13,13));
    }
    
}
