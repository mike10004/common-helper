package com.github.mike10004.common.image;

import com.google.common.io.ByteSource;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * Class that provides static utility methods relating to image info objects.

 * @see ImageInfo
 */
public class ImageInfos {

    private ImageInfos() {}
    
    /**
     * Reads the image size from a byte array containing image data. This 
     * first attempts to use {@link ImageInfo} to read just the header data,
     * but if that fails the whole image is buffered into memory with
     * {@link ImageIO#read(java.io.InputStream) }. In any case, this method
     * never throws an exception; instead, if the image data is unreadable,
     * it returns a dimension object with zero width and height.
     * 
     * <p>In common deployments, the fallback of loading the image into 
     * memory probably never works, because {@code ImageInfo}'s support is 
     * a superset of JDK image format support. But some installations may
     * have extra codecs installed, and it's nice to make use of those if 
     * they're there.
     * @param bytes the image data bytes
     * @return the image dimensions
     */
    public static Dimension readImageSize(byte[] bytes) {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ImageInfo ii = new ImageInfo();
        ii.setInput(in);
        if (ii.check()) {
            return new Dimension(ii.getWidth(), ii.getHeight());
        } else {
            in.reset();
            BufferedImage image;
            try {
                image = ImageIO.read(in);
                if (image == null) {
                    throw new IOException("failed to read image; format is probably not supported");
                }
                return new Dimension(image.getWidth(), image.getHeight());
            } catch (IOException ex) {
                Integer m1 = bytes.length > 0 ? Integer.valueOf(bytes[0]) : null;
                Integer m2 = bytes.length > 1 ? Integer.valueOf(bytes[1]) : null;
                Logger.getLogger(ImageInfos.class.getName())
                        .log(Level.FINER, "reading image failed on array of "
                                + "length {0} with magic number {1} {2}: {3}", 
                                new Object[]{bytes.length, m1, m2, ex});
                return new Dimension(0, 0);
            }
        }
    }
    
    /**
     * Reads image info from a stream.
     * @param in the input stream containing image data
     * @return the image info object, with {@link ImageInfo#check() } 
     * already invoked
     * @throws IOException if {@link ImageInfo#check() } fails or any other
     * I/O exception is thrown
     */
    public static ImageInfo read(InputStream in) throws IOException {
        ImageInfo imageInfo = new ImageInfo();
        imageInfo.setInput(in);
        if (!imageInfo.check()) {
            throw new IOException("ImageInfo.check() failed; data stream is "
                    + "broken or does not contain data in a supported image format");
        }
        return imageInfo;
    }
    
    /**
     * Opens a stream and reads image info from it. The stream is closed
     * afterwards. This method invokes {@link #read(java.io.InputStream) }.
     * @param byteSource the byte source
     * @return the image info
     * @throws IOException if reading fails
     */
    public static ImageInfo read(ByteSource byteSource) throws IOException {
        try (InputStream in = byteSource.openStream()) {
            return read(in);
        }
    }
    
}
