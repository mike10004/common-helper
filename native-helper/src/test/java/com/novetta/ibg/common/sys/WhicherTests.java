/*
 * (c) 2014 IBG, A Novetta Solutions Company.
 */
package com.novetta.ibg.common.sys;

import com.google.common.base.Optional;
import java.io.File;
import java.io.IOException;
import static org.junit.Assert.*;

/**
 *
 * @author mchaberski
 */
public class WhicherTests {

    public static void testSomeFileFound(File temporaryDir) throws IOException {
        System.out.println("testSomeFileFound");
        
        Whicher w = Whicher.builder(temporaryDir).build();
        
        File file = File.createTempFile("helloworld", ".tmp", temporaryDir);
        try {
            Optional<File> result = w.which(file.getName());
            System.out.format("%s -> %s%n", file.getName(), result);
            assertTrue(result.isPresent());
        } finally {
            file.delete();
        }
        
        String absent = "thisFileProbablyDoesNotExist";
        Optional<File> result = w.which(absent);
        System.out.format("%s -> %s%n", absent, result);
        assertFalse(result.isPresent());
    }
    
}
