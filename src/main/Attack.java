package main;

import ij.ImagePlus;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

public class Attack {
    public static int jpegQuality;

    public static BufferedImage rotateImage(BufferedImage image, int degrees) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage newImageFromBuffer = new BufferedImage(width, height, image.getType());

        Graphics2D graphics2D = newImageFromBuffer.createGraphics();
        graphics2D.rotate(Math.toRadians(degrees), (double) width / 2, (double) height / 2);
        graphics2D.drawImage(image, null, 0, 0);
        new ImagePlus("Rotation by " + degrees + "°", newImageFromBuffer).show();
        return newImageFromBuffer;
    }

    public static BufferedImage rotateImage45(BufferedImage image) {
        return rotateImage(image, 45);
    }

    public static BufferedImage rotateImage90(BufferedImage image) {
        return rotateImage(image, 90);
    }


    public static BufferedImage jpegCompression(BufferedImage image) {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        BufferedImage bufferedImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(compressed)) {
            ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("JPEG").next();
            ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
            jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            jpgWriteParam.setCompressionQuality(jpegQuality / 100F);
            jpgWriter.setOutput(outputStream);
            jpgWriter.write(null, new IIOImage(image, null, null), jpgWriteParam);
            jpgWriter.dispose();
            InputStream is = new ByteArrayInputStream(compressed.toByteArray());
            bufferedImage = ImageIO.read(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        new ImagePlus("JPEG " + jpegQuality + "%", bufferedImage).show();
        return bufferedImage;
    }
}
