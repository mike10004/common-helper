/*
 * (c) 2014 IBG, A Novetta Solutions Company.
 */
package com.novetta.ibg.common.sys;

import com.google.common.base.Optional;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FilenameUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author mchaberski
 */
public class WhicherWindowsTest {
    
    @Rule 
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    private static boolean skip;
    
    public WhicherWindowsTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        skip = !Platforms.getPlatform().isWindows();
        
    }

    @Test
    public void testGnu() {
        if (skip) return;
        System.out.println("gnu");
        
        Whicher w = Whicher.gnu();
        Optional<File> result;
        String searchString = "cmd";
        result = w.which(searchString);
        System.out.format("which '%s' = %s%n", searchString, result);
        assertTrue(result.isPresent());
        assertTrue("unexpected cmd resolution", 
                FilenameUtils.equalsNormalizedOnSystem(
                "c:\\windows\\system32\\cmd.exe",
                result.get().getAbsolutePath()));
        
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
