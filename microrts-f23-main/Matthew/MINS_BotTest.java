package mins_bot;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */

import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import ai.evaluation.EvaluationFunction;
import java.util.List;
import mins_bot.MINS_Bot;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.Unit;
import rts.units.UnitTypeTable;

/**
 *
 * @author Matthew
 */
public class MINS_BotTest {
    
    public MINS_BotTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testGetActionIntegrity() throws Exception {
        int MAXCYCLES = 1000;
        int PERIOD = 10;
        boolean gameover = false;
        UnitTypeTable utt = new UnitTypeTable();
        PhysicalGameState pgs;
        pgs = PhysicalGameState.load("maps/16x16/basesWorkers16x16.xml", utt);
        GameState gs = new GameState(pgs, utt);
        
        AIWithComputationBudget ai1 = new MINS_Bot(utt);
        AIWithComputationBudget ai2 = new MINS_Bot(utt);
        ai1.setTimeBudget(PERIOD);
        ai2.setTimeBudget(PERIOD);
        
          do{//test to ensure the validity of actions returned
                PlayerAction pa1 = ai1.getAction(0, gs);
                PlayerAction pa2 = ai2.getAction(1, gs);
                gs.issueSafe(pa1);
                gs.issueSafe(pa2);
                assertTrue(gs.integrityCheck());

                // simulate:
                gameover = gs.cycle();
                
        }while(!gameover && gs.getTime()<MAXCYCLES);
         
    }
    
     @Test
    public void testGetActionTime() throws Exception {
        int MAXCYCLES = 1000;
        int PERIOD = 10;
        boolean gameover = false;
        UnitTypeTable utt = new UnitTypeTable();
        PhysicalGameState pgs;
        pgs = PhysicalGameState.load("maps/16x16/basesWorkers16x16.xml", utt);
        GameState gs = new GameState(pgs, utt);
        
        AIWithComputationBudget ai1 = new MINS_Bot(utt);
        AIWithComputationBudget ai2 = new MINS_Bot(utt);
        ai1.setTimeBudget(PERIOD);
        ai2.setTimeBudget(PERIOD);        
        
        do{//test to ensure time is not exceeded
            long startTime = System.nanoTime();
            PlayerAction pa1 = ai1.getAction(0, gs);
            assertFalse(startTime - System.nanoTime() > PERIOD*1000000);
            startTime = System.nanoTime();
            PlayerAction pa2 = ai2.getAction(1, gs);
            assertFalse(startTime - System.nanoTime() > PERIOD*1000000);
            gs.issue(pa1);
            gs.issue(pa2);

            gameover = gs.cycle();
        }while(!gameover && gs.getTime()<MAXCYCLES);
        
    }
    
    @Test
    public void testGetActionAgainstAI() throws Exception {
        int MAXCYCLES = 1000;
        int PERIOD = 10;
        boolean gameover = false;
        UnitTypeTable utt = new UnitTypeTable();
        PhysicalGameState pgs;
        pgs = PhysicalGameState.load("maps/16x16/basesWorkers16x16.xml", utt);
        GameState gs = new GameState(pgs, utt);
        
        AIWithComputationBudget ai1 = new MINS_Bot(utt);
        AIWithComputationBudget ai2 = new mayariBot.mayari(utt);
        ai1.setTimeBudget(PERIOD);     
        
        do{//test to ensure time is not exceeded
            long startTime = System.nanoTime();
            PlayerAction pa1 = ai1.getAction(0, gs);
            assertFalse(startTime - System.nanoTime() > PERIOD*1000000);
            PlayerAction pa2 = ai2.getAction(1, gs);
            gs.issue(pa1);
            gs.issue(pa2);
            assertTrue(gs.integrityCheck());

            gameover = gs.cycle();
        }while(!gameover && gs.getTime()<MAXCYCLES);
        
    }
}