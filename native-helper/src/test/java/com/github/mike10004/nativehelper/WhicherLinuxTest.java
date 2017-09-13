package com.github.mike10004.nativehelper;

import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class WhicherLinuxTest {
    
    @Rule 
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    private static final ImmutableSet<String> expectedGrepPathnames = ImmutableSet.of("/bin/grep", "/usr/bin/grep");

    @BeforeClass
    public static void setUpClass() {
        Assume.assumeTrue(Platforms.getPlatform().isLinux());
    }

    @Test
    public void testGnu() {
        System.out.println("gnu");
        
        Whicher w = Whicher.gnu();
        java.util.Optional<File> result;
        String searchString = "grep";
        result = w.which(searchString);
        System.out.format("which '%s' = %s%n", searchString, result);
        assertTrue("grep executable must be found", result.isPresent());
        String grepAbsPath = result.get().getAbsolutePath();
        assertTrue("expect grep in one of these pathnames: " + expectedGrepPathnames, 
                expectedGrepPathnames.contains(grepAbsPath));
        
        
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
