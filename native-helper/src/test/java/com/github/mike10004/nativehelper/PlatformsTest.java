package com.github.mike10004.nativehelper;

import com.github.mike10004.nativehelper.Platforms.NullPlatform;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 *
 * @author mchaberski
 */
public class PlatformsTest {
    
    public PlatformsTest() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testLinux() {
        System.out.println("testLinux");
        if (Platforms.getPlatform().isWindows()) {
            System.out.println("skipping test");
            return;
        }
        String osName = "Linux";
        Platform p = Platforms.getPlatform(osName);
        File dir;
        assertEquals(new File("/etc"), dir = p.getSystemConfigDir());
        System.out.println("getSystemConfigDir = " + dir);
        assertEquals(new File("/usr/share"), dir = p.getCommonProgramFilesDir());
        System.out.println("getCommonProgramFilesDir = " + dir);
        assertEquals(new File("/var/lib"), dir = p.getProgramDataDir());
        System.out.println("getProgramDataDir = " + dir);
        String userHomeDir = System.getProperty("user.home");
        assertEquals(new File(userHomeDir), dir = p.getUserConfigDir());
        System.out.println("getUserConfigDir = " + dir);
        
        File programConfigDir = p.getUserConfigDir("hello", "world");
        File expected = new File(userHomeDir, ".hello/world");
        System.out.println("getUserHomeConfigDir(hello,world) = " + programConfigDir);
        assertEquals(expected, programConfigDir);
        
        
        File programSystemConfigDir = p.getSystemConfigDir("hello");
        System.out.println("programSystemConfigDir = " + programSystemConfigDir);
        assertEquals(new File("/etc/hello"), programSystemConfigDir);
    }
    
    @Test
    public void testSupportedOsNames() throws Exception {
        System.out.println("testSupportedOsNames");
        
        List<String> supportedOsNames = Arrays.asList(
                "FreeBSD",
                "Linux",
                "Windows 2003",
                "Windows Server 2003",
                "Windows 2008", 
                "Windows Server 2008",
                "Windows 7",
                "Windows 8",
                "Windows Vista",
                "Windows Unknown",
                "Mac OS X"
        );
        
        for (String osName : supportedOsNames) {
            Platform platform = Platforms.getPlatform(osName);
            System.out.format("'%s' -> %s%n", osName, platform);
            assertFalse(platform instanceof NullPlatform);
            
        }
    }
}
