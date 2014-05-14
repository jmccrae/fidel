/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.monnetproject.translation.fidel;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.io.File;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jmccrae
 */
public class SQLLanguageModelTest {
    
    public SQLLanguageModelTest() {
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
        final File file = new File("src/test/resources/model.lm.db");
        if(file.exists()) {
            file.delete();
        }
    }

    /**
     * Test of get method, of class SQLLanguageModel.
     */
    @Test
    public void testGet() throws Exception {
        System.out.println("get");
        final IntegerLanguageModel instance = new SQLLanguageModel.Factory().getModel(new File("src/test/resources/model.lm"));
        int[] pint = { 1, 2 };
        Phrase phrase = new Phrase(pint);
        double[] expResult = { -1, -1 };
        double[] result = instance.get(phrase);
        assertArrayEquals(expResult, result, 0.0);
    }

    /**
     * Test of close method, of class SQLLanguageModel.
     */
    @Test
    public void testClose() {
        System.out.println("close");
        IntegerLanguageModel instance = new SQLLanguageModel.Factory().getModel(new File("src/test/resources/model.lm"));
        instance.close();
    }

    /**
     * Test of order method, of class SQLLanguageModel.
     */
    @Test
    public void testOrder() {
        System.out.println("order");
        SQLLanguageModel instance = new SQLLanguageModel.Factory().getModel(new File("src/test/resources/model.lm"));
        int expResult = 3;
        int result = instance.order();
        assertEquals(expResult, result);
    }

    /**
     * Test of wordMap method, of class SQLLanguageModel.
     */
    @Test
    public void testWordMap() {
        System.out.println("wordMap");
        SQLLanguageModel instance = new SQLLanguageModel.Factory().getModel(new File("src/test/resources/model.lm"));
        Object2IntMap result = instance.wordMap();
        assertNotNull(result);
    }

    /**
     * Test of invWordMap method, of class SQLLanguageModel.
     */
    @Test
    public void testInvWordMap() {
        System.out.println("invWordMap");
        SQLLanguageModel instance = new SQLLanguageModel.Factory().getModel(new File("src/test/resources/model.lm"));
        Int2ObjectMap result = instance.invWordMap();
        assertNotNull(result);
    }
}