package com.github.mike10004.nativehelper;

import com.google.common.base.Optional;
import static com.google.common.base.Preconditions.checkState;
import java.io.File;
import java.io.IOException;
import static org.junit.Assert.*;

/**
 *
 * @author mchaberski
 */
public class WhicherTests {

    public static void testWhich_found(File temporaryDir) throws IOException {
        System.out.println("testWhich_found");
        
        Whicher w = Whicher.builder(temporaryDir).build();
        
        File file = File.createTempFile("helloworld", ".tmp", temporaryDir);
        Optional<File> result = w.which(file.getName());
        System.out.format("%s -> %s%n", file.getName(), result);
        assertTrue("expect result present", result.isPresent());
        assertTrue("expect result isFile true", result.get().isFile());
    }
    
    public static void testWhich_notFound(File temporaryDir) throws IOException {
        System.out.println("testWhich_NotFound");
        String absent = "thisFileProbablyDoesNotExist";
        File pathname = new File(temporaryDir, absent);
        checkState(!pathname.exists(), "trying to confirm nonexistence, but exists: %s", pathname);
        Whicher w = Whicher.builder(temporaryDir).build();
        Optional<File> result = w.which(absent);
        System.out.format("%s -> %s%n", absent, result);
        assertFalse("expect absent result", result.isPresent());
    }
    
}
