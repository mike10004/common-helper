package com.github.mike10004.nativehelper;

import org.apache.commons.io.FilenameUtils;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WhicherWindowsTest {
    
    @Rule 
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    @BeforeClass
    public static void setUpClass() {
        Assume.assumeTrue(Platforms.getPlatform().isWindows());
    }

    @Test
    public void testGnu() {
        System.out.println("gnu");
        
        Whicher w = Whicher.gnu();
        java.util.Optional<File> result;
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
