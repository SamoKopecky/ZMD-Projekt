package main;

import Jama.Matrix;
import ij.ImagePlus;
import main.enums.Component;

import java.awt.image.BufferedImage;
import java.util.function.Function;

public class Watermark {
    public int width;
    public int height;
    public Matrix watermarkPixels;
    public int bitDepth;
    public ColorTransform cTrans;
    public Process process;
    public Matrix[][] blocks;
    public int size = 8;
    public int heightBlocks;
    public int widthBlocks;
    public ColorTransform originalWatermark;
    public ColorTransform extractedWatermark;

    public Watermark(int bitDepth) {
        this.bitDepth = bitDepth;
        process = new Process(new ImagePlus("Lenna.png"));
        cTrans = process.getcTrans();
        BufferedImage bImage = new ImagePlus("watermark.png").getBufferedImage();
        originalWatermark = new ColorTransform(bImage);
        width = bImage.getWidth();
        height = bImage.getHeight();
        watermarkPixels = new Matrix(height, width);
        heightBlocks = height / size;
        widthBlocks = width / size;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                watermarkPixels.set(i, j, bImage.getColorModel().getBlue(bImage.getRGB(i, j)));
            }
        }
    }

    public BufferedImage insertSpaceWatermark(Component component) {
        int[][] data = cTrans.getRgbComponents().get(component);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                data[i][j] = setZeroAtBitDepth(data[i][j]) | setOneAtBitDepth((int) watermarkPixels.get(j, i));
            }
        }
        cTrans.getRgbSetters().get(component).accept(data);
        cTrans.createImageFromRgb("Inserted watermark").show();
        return cTrans.createBufferedImageFromRgb();
    }

    public ImagePlus extractSpaceWatermark(Component component) {
        int[][] data = cTrans.getRgbComponents().get(component);
        int[][] watermark = new int[height][width];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                watermark[i][j] = blackOrWhite(data[i][j] & setOneAtBitDepth(255));
            }
        }
        return createImagePlus(watermark, width, height);
    }

    public void insertTranWatermark(Function<Integer, Matrix> function, int u1, int v1, int u2, int v2) {
        int counter = fillBlocks(function);

        Matrix[] markedBlocks = new Matrix[counter];
        Matrix scaledWatermarkPixels = scaleWatermarkPixels(this.watermarkPixels, cTrans::downSample, widthBlocks);
        counter = 0;
        for (int i = 0; i < heightBlocks; i++) {
            for (int j = 0; j < widthBlocks; j++) {
                insertWatermark(u1, v1, u2, v2, i, j, scaledWatermarkPixels);
                markedBlocks[counter] = this.blocks[i][j];
                counter++;
            }
        }
        cTrans.setBlocksY(markedBlocks);
        process.inverseBlocks(function.apply(size));
        process.mergeBlocksIntoComponent(size);
        process.showImage("Inserted watermark");
    }

    public ImagePlus extractTranWatermark(Function<Integer, Matrix> function, int u1, int v1, int u2, int v2) {
        fillBlocks(function);
        Matrix watermark = new Matrix(heightBlocks, widthBlocks);
        for (int i = 0; i < heightBlocks; i++) {
            for (int j = 0; j < widthBlocks; j++) {
                Matrix block = this.blocks[i][j];
                double b1 = block.get(u1, v1);
                double b2 = block.get(u2, v2);
                if (b1 > b2) {
                    watermark.set(i, j, 0);
                } else {
                    watermark.set(i, j, 255);
                }
            }
        }
        int[][] watermarkArr = new int[height][width];
        Matrix scaledWatermarkPixels = scaleWatermarkPixels(watermark, cTrans::upSample, width);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                watermarkArr[i][j] = (int) scaledWatermarkPixels.get(i, j);
            }
        }
        return createImagePlus(watermarkArr, width, height);
    }

    private ImagePlus createImagePlus(int[][] watermark, int width, int height) {
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ColorTransform.bImageFromRgb(bufferedImage, watermark, watermark, watermark, height, width);
        extractedWatermark = new ColorTransform(bufferedImage);
        return new ImagePlus("Extracted watermark", bufferedImage);
    }

    private int fillBlocks(Function<Integer, Matrix> function) {
        process.divideIntoBlocks(size);
        process.transformBlocks(function.apply(size));
        Matrix[] originalBlocks = cTrans.getBlocksY();

        Matrix[][] dividedOriginalBlocks = new Matrix[heightBlocks][widthBlocks];
        int counter = 0;
        for (int i = 0; i < heightBlocks; i++) {
            for (int j = 0; j < widthBlocks; j++) {
                dividedOriginalBlocks[i][j] = originalBlocks[counter];
                counter++;
            }
        }
        this.blocks = dividedOriginalBlocks;
        return counter;
    }


    private Matrix scaleWatermarkPixels(Matrix watermark, Function<Matrix, Matrix> scale, int target) {
        while (watermark.getRowDimension() != target) {
            watermark = scale.apply(watermark);
            watermark = watermark.transpose();
            watermark = scale.apply(watermark);
            watermark = watermark.transpose();
        }
        return watermark;
    }

    private void insertWatermark(int u1, int v1, int u2, int v2, int i, int j, Matrix watermark) {
        Matrix block = this.blocks[i][j];
        double b1 = block.get(u1, v1);
        double b2 = block.get(u2, v2);
        if (watermark.get(j, i) == 0) {
            if (!(b1 > b2)) {
                block.set(u1, v1, b2);
                block.set(u2, v2, b1);
            }
        } else {
            if (!(b1 <= b2)) {
                block.set(u1, v1, b2);
                block.set(u2, v2, b1);
            }
        }
        b1 = block.get(u1, v1);
        b2 = block.get(u2, v2);
        if (!(Math.abs(b1 - b2) > bitDepth)) {
            if (b1 < b2) {
                block.set(u1, v1, b1 - (double) bitDepth / 2);
                block.set(u2, v2, b2 + (double) bitDepth / 2);
            } else {
                block.set(u1, v1, b1 + (double) bitDepth / 2);
                block.set(u2, v2, b2 - (double) bitDepth / 2);
            }
        }
        this.blocks[i][j] = block;
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
