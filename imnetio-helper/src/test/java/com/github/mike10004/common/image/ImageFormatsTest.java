package com.github.mike10004.common.image;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.github.mike10004.common.image.ImageInfo.Format;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;

public class ImageFormatsTest {

    @Test
    public void guessFormat() throws Exception {
        Map<byte[], Optional<Format>> testCases = ImmutableMap.<byte[], Optional<Format>>builder()
                .put(Resources.toByteArray(getClass().getResource("/images/logo.gif")), Optional.of(Format.GIF))
                .put(Resources.toByteArray(getClass().getResource("/images/logo.png")), Optional.of(Format.PNG))
                .put(Resources.toByteArray(getClass().getResource("/images/logo.jpg")), Optional.of(Format.JPEG))
                .put(Resources.toByteArray(getClass().getResource("/images/logo.jpeg")), Optional.of(Format.JPEG))
                .put(Resources.toByteArray(getClass().getResource("/images/logo.pgm")), Optional.of(Format.PGM))
                .put(Resources.toByteArray(getClass().getResource("/images/logo.ppm")), Optional.of(Format.PPM))
                .put(Resources.toByteArray(getClass().getResource("/images/logo.ras")), Optional.of(Format.RAS))
                .put(new byte[]{1, 2, 3, 4}, Optional.empty())
                .put(new byte[0], Optional.empty())
                .put("not an image".getBytes(StandardCharsets.UTF_8), Optional.empty())
                .build();
        for (byte[] input : testCases.keySet()) {
            Optional<Format> expected = testCases.get(input);
            assertEquals("byte array of length " + input.length, expected.orElse(null), ImageFormats.guessFormat(input));
        }
    }

    @org.junit.Ignore("tiff format detection is not currently supported") // TODO fix tiff format detection
    @Test
    public void guessFormat_tiff() throws Exception {
        assertEquals("tiff", Format.TIFF, ImageFormats.guessFormat(Resources.toByteArray(getClass().getResource("/images/logo.tiff"))));
    }

    @Test
    public void guessMimeType() throws Exception {
        Map<byte[], Optional<String>> testCases = ImmutableMap.<byte[], Optional<String>>builder()
                .put(Resources.toByteArray(getClass().getResource("/images/logo.gif")), Optional.of(Format.GIF.getMimeType()))
                .put(Resources.toByteArray(getClass().getResource("/images/logo.png")), Optional.of(Format.PNG.getMimeType()))
                .put(Resources.toByteArray(getClass().getResource("/images/logo.jpg")), Optional.of(Format.JPEG.getMimeType()))
                .put(Resources.toByteArray(getClass().getResource("/images/logo.jpeg")), Optional.of(Format.JPEG.getMimeType()))
                .put(Resources.toByteArray(getClass().getResource("/images/logo.pgm")), Optional.of(Format.PGM.getMimeType()))
                .put(Resources.toByteArray(getClass().getResource("/images/logo.ppm")), Optional.of(Format.PPM.getMimeType()))
                .put(Resources.toByteArray(getClass().getResource("/images/logo.ras")), Optional.of(Format.RAS.getMimeType()))
                .put(new byte[]{1, 2, 3, 4}, Optional.empty())
                .put(new byte[0], Optional.empty())
                .put("not an image".getBytes(StandardCharsets.UTF_8), Optional.empty())
                .build();
        for (byte[] input : testCases.keySet()) {
            Optional<String> expected = testCases.get(input);
            assertEquals("byte array of length " + input.length, expected.orElse(null), ImageFormats.guessMimeType(input));
        }
    }

}