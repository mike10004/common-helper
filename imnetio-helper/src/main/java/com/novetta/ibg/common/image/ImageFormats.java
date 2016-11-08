/*
 * (c) 2015 Mike Chaberski
 */
package com.novetta.ibg.common.image;

import com.google.common.base.Optional;
import java.io.ByteArrayInputStream;

/**
 * Class that provides static utility methods relating to image formats.
 * @author mchaberski
 * @see ImageInfo.Format
 */
public class ImageFormats {

    public static Optional<ImageInfo.Format> guessFormat(byte[] imageData) {
        ImageInfo ii = new ImageInfo();
        ii.setInput(new ByteArrayInputStream(imageData));
        if (!ii.check()) {
            return Optional.absent();
        }
        return Optional.of(ii.getFormat());
    }
    
    public static Optional<String> guessMimeType(byte[] imageData) {
        Optional<ImageInfo.Format> format = guessFormat(imageData);
        if (format.isPresent()) {
            return getMimeTypeFromFormat(format.get());
        } else {
            return Optional.absent();
        }
    }
    
    public static Optional<String> getMimeTypeFromFormat(ImageInfo.Format imageInfoFormat) {
        return Optional.fromNullable(getMimeTypeOrNull(imageInfoFormat));
    }
    
    private static String getMimeTypeOrNull(ImageInfo.Format imageInfoFormat) {
        switch (imageInfoFormat) {
            case ICO: 
                return "image/vnd.microsoft.icon";
            case PGM:
                return "image/x-portable-graymap";
            case PPM:
                return "image/x-portable-pixmap";
            default:
                return "image/" + imageInfoFormat.name().toLowerCase();
        }
    }
}
