/*
 * (c) 2015 Mike Chaberski
 */

package com.github.mike10004.common.image;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mchaberski
 */
public class ImageInfosTest {
    
    private static final int minExpectedSuccesses = 10;

    private static final ImmutableList<String> testImageFilenames = ImmutableList.<String>builder()
            .add("logo.gif")
            .add("logo.ico")
            .add("logo.jbig")
            .add("logo.jp2")
            .add("logo.jpeg")
            .add("logo.jpg")
            .add("logo.pbm")
            .add("logo.pcx")
            .add("logo.pgm")
            .add("logo.pict")
            .add("logo.pix")
            .add("logo.png")
            .add("logo.ppm")
            .add("logo.ras")
            .add("logo.tiff")
            .add("logo.xpm")
            .build();
    
    @Test
    public void testReadImageInfo() throws URISyntaxException {
        Set<String> successes = new TreeSet<>(), failures = new TreeSet<>();
        for (String imageFilename : testImageFilenames) {
            File imageFile = getImageFile(imageFilename);
            assertTrue(imageFile.isFile());
            try {
                ImageInfo ii = ImageInfos.read(Files.asByteSource(imageFile));
                successes.add(imageFilename);
                System.out.format("%s %dx%d%n", imageFile.getName(), ii.getWidth(), ii.getHeight());
            } catch (IOException e) {
                failures.add(imageFilename);
                System.out.format("%s %s%n", imageFile.getName(), e);
            }
        }
        System.out.format("%d successes, %d failures%n", successes.size(), failures.size());
        System.out.println("successes: " + successes);
        System.out.println(" failures: " + failures);
        assertTrue(successes.size() >= minExpectedSuccesses);
    }
    
    @Test
    public void testReadImageSize() throws IOException, URISyntaxException {
        Set<String> successes = new TreeSet<>(), failures = new TreeSet<>();
        for (String imageFilename : testImageFilenames) {
            File imageFile = getImageFile(imageFilename);
            assertTrue(imageFile.isFile());
            byte[] bytes = Files.toByteArray(imageFile);
            Dimension dims = ImageInfos.readImageSize(bytes);
            System.out.format("%s %dx%d%n", imageFile.getName(), dims.width, dims.height);
            if (dims.width != 0) {
                successes.add(imageFilename);
            } else {
                failures.add(imageFilename);
            }
        }
        System.out.format("%d successes, %d failures%n", successes.size(), failures.size());
        System.out.println("successes: " + successes);
        System.out.println(" failures: " + failures);
        assertTrue(successes.size() >= minExpectedSuccesses);
    }
    
    protected File getImageFile(String imageFilename) throws URISyntaxException {
        URL resource = getClass().getResource("/images/" + imageFilename);
        File file = new File(resource.toURI());
        return file;
    }
}

