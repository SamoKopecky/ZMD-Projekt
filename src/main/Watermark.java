package main;

import ij.ImagePlus;
import main.enums.Component;

import java.awt.image.BufferedImage;

public class Watermark {
    public BufferedImage bImage;
    public int width;
    public int height;
    public int[][] pixels;
    public int bitDepth;
    public ColorTransform colorTransform;

    public Watermark(int bitDepth) {
        this.bitDepth = bitDepth;
        this.colorTransform = new ColorTransform(new ImagePlus("Lenna.png").getBufferedImage());
        bImage = new ImagePlus("watermark.png").getBufferedImage();
        width = bImage.getWidth();
        height = bImage.getHeight();
        pixels = new int[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                pixels[i][j] = bImage.getColorModel().getBlue(bImage.getRGB(i, j));
            }
        }
    }

    public void putWatermark(Component component) {
        int[][] data = colorTransform.getRgbComponents().get(component);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                data[i][j] = setZeroAtBitDepth(data[i][j]) | setOneAtBitDepth(pixels[j][i]);
            }
        }
        colorTransform.getRgbSetters().get(component).accept(data);
        colorTransform.createImageFromRgb().show();
    }

    public ImagePlus extractWatermark(Component component) {
        int[][] data = colorTransform.getRgbComponents().get(component);
        int[][] watermark = new int[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                watermark[i][j] = blackOrWhite(data[i][j] & setOneAtBitDepth(255));
            }
        }
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ColorTransform.bImageFromRgb(bufferedImage, watermark, watermark, watermark, height, width);
        return new ImagePlus("watermark", bufferedImage);
    }

    private int setZeroAtBitDepth(int value) {
        return (~(1 << bitDepth - 1)) & value;
    }

    private int setOneAtBitDepth(int value) {
        return 1 << bitDepth - 1 & value;
    }

    private int blackOrWhite(int value) {
        if (value > 0) {
            return 255;
        }
        return 0;
    }
}
