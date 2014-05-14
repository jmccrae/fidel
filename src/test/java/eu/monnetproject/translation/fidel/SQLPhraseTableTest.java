/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.monnetproject.translation.fidel;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
public class SQLPhraseTableTest {
    
    public SQLPhraseTableTest() {
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
        final File file = new File("src/test/resources/test.pt.db");
        if(file.exists()) {
            file.delete();
        }
    }

    /**
     * Test of getForeignLanguage method, of class SQLPhraseTable.
     */
    @Test
    public void testGetForeignLanguage() {
        System.out.println("getForeignLanguage");
        SQLPhraseTable instance = new SQLPhraseTable.Factory().getPhraseTable("x", "y", new String[] { "a", "b", "c", "d", "e" }, new File("src/test/resources/test.pt"));
        String expResult = "x";
        String result = instance.getForeignLanguage();
        assertEquals(expResult, result);
    }

    /**
     * Test of getTranslationLanguage method, of class SQLPhraseTable.
     */
    @Test
    public void testGetTranslationLanguage() {
        System.out.println("getTranslationLanguage");
        SQLPhraseTable instance = new SQLPhraseTable.Factory().getPhraseTable("x", "y", new String[] { "a", "b", "c", "d", "e" }, new File("src/test/resources/test.pt"));
        String expResult = "y";
        String result = instance.getTranslationLanguage();
        assertEquals(expResult, result);
    }

    /**
     * Test of lookup method, of class SQLPhraseTable.
     */
    @Test
    public void testLookup() {
        System.out.println("lookup");
        List<String> terms = Arrays.asList(new String[] { "A", "B" });
        SQLPhraseTable instance = new SQLPhraseTable.Factory().getPhraseTable("x", "y", new String[] { "a", "b", "c", "d", "e" }, new File("src/test/resources/test.pt"));
        List<PhraseTable.PhraseTableEntry> result = (List< PhraseTable.PhraseTableEntry>)instance.lookup(terms);
        for(PhraseTable.PhraseTableEntry r : result) {
            System.err.println(r.toString());
        }
        assertEquals(2, result.size());
    }
}