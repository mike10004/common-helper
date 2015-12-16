/*
 * (c) 2014 IBG, A Novetta Solutions Company.
 */
package com.novetta.ibg.common.sys;

import com.google.common.base.Optional;
import java.io.File;
import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author mchaberski
 */
public class WhicherLinuxTest {
    
    @Rule 
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    private static boolean skip;
    
    public WhicherLinuxTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        skip = !Platforms.getPlatform().isLinux();
        
    }

    @Test
    public void testGnu() {
        if (skip) return;
        System.out.println("gnu");
        
        Whicher w = Whicher.gnu();
        Optional<File> result;
        String searchString = "grep";
        result = w.which(searchString);
        System.out.format("which '%s' = %s%n", searchString, result);
        assertTrue(result.isPresent());
        assertEquals(result.get().getAbsolutePath(), "/bin/grep");
        
        searchString = "notanexecutable";
        result = w.which(searchString);
        System.out.format("which '%s' = %s%n", searchString, result);
        boolean present = result.isPresent();
        assertFalse("should not have found " + searchString, present);
    }
    
    @Test
    public void testWhich_found() throws IOException {
        System.out.println("testWhich_found");
        WhicherTests.testWhich_found(temporaryFolder.newFolder());
    }

    @Test
    public void testWhich_notFound() throws IOException {
        System.out.println("testWhich_notFound");
        WhicherTests.testWhich_notFound(temporaryFolder.newFolder());
    }
}
