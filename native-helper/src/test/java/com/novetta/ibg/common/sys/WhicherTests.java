/*
 * (c) 2014 IBG, A Novetta Solutions Company.
 */
package com.novetta.ibg.common.sys;

import com.google.common.base.Optional;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import static org.junit.Assert.*;

/**
 *
 * @author mchaberski
 */
public class WhicherTests {

    static void testSomeFileFound() throws IOException {
        System.out.println("testSomeFileFound");
        
        File dir = FileUtils.getTempDirectory();
        Whicher w = Whicher.builder(dir).build();
        
        File file = File.createTempFile("helloworld", ".tmp", dir);
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
