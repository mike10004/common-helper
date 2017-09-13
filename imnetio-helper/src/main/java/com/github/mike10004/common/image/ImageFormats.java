/*
 * (c) 2015 Mike Chaberski
 */
package com.github.mike10004.common.image;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;

/**
 * Class that provides static utility methods relating to image formats.
 * @author mchaberski
 * @see ImageInfo.Format
 */
public class ImageFormats {

    @Nullable
    public static ImageInfo.Format guessFormat(byte[] imageData) {
        ImageInfo ii = new ImageInfo();
        ii.setInput(new ByteArrayInputStream(imageData));
        if (!ii.check()) {
            return null;
        }
        return ii.getFormat();
    }

    @Nullable
    public static String guessMimeType(byte[] imageData) {
        @Nullable ImageInfo.Format format = guessFormat(imageData);
        if (format != null) {
            return getMimeTypeFromFormat(format);
        } else {
            return null;
        }
    }

    public static String getMimeTypeFromFormat(ImageInfo.Format imageInfoFormat) {
        return getMimeTypeOrNull(imageInfoFormat);
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
