package main;

import Jama.Matrix;
import ij.ImagePlus;

import main.enums.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class ColorTransform {
    private int[][] red;
    private int[][] green;
    private int[][] blue;

    private final int imageHeight;
    private final int imageWidth;

    private Matrix y;
    private Matrix Cb;
    private Matrix Cr;

    private BufferedImage bImage;
    private final ColorModel colorModel;

    private final HashMap<Component, Matrix> notRgbComponents = new HashMap<>();
    private final HashMap<Component, int[][]> RgbComponents = new HashMap<>();
    private final HashMap<Component, Consumer<int[][]>> RgbSetters = new HashMap<>();

    private Matrix[] blocksY;
    private Matrix[] blocksCb;
    private Matrix[] blocksCr;

    public ColorTransform(BufferedImage bImage) {
        this.bImage = bImage;
        this.colorModel = bImage.getColorModel();
        this.imageHeight = bImage.getHeight();
        this.imageWidth = bImage.getWidth();

        red = new int[imageHeight][imageWidth];
        green = new int[imageHeight][imageWidth];
        blue = new int[imageHeight][imageWidth];

        y = new Matrix(this.imageHeight, this.imageWidth);
        Cb = new Matrix(this.imageHeight, this.imageWidth);
        Cr = new Matrix(this.imageHeight, this.imageWidth);

        RgbSetters.put(Component.RED, this::setRed);
        RgbSetters.put(Component.BLUE, this::setBlue);
        RgbSetters.put(Component.GREEN, this::setGreen);


        updateMaps();
        fromImageToRgb();
    }

    public void updateMaps() {
        notRgbComponents.put(Component.Y, y);
        notRgbComponents.put(Component.Cb, Cb);
        notRgbComponents.put(Component.Cr, Cr);
        RgbComponents.put(Component.RED, red);
        RgbComponents.put(Component.GREEN, green);
        RgbComponents.put(Component.BLUE, blue);
    }

    public void fromImageToRgb() {
        for (int i = 0; i < imageHeight; i++) {
            for (int j = 0; j < imageWidth; j++) {
                red[i][j] = colorModel.getRed(bImage.getRGB(j, i));
                green[i][j] = colorModel.getGreen(bImage.getRGB(j, i));
                blue[i][j] = colorModel.getBlue(bImage.getRGB(j, i));
            }
        }
    }

    public ImagePlus createImageFromRgb(String name) {
        BufferedImage bImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        bImageFromRgb(bImage, red, green, blue, imageHeight, imageWidth);
        return new ImagePlus(name, bImage);
    }

    public BufferedImage createBufferedImageFromRgb() {
        BufferedImage bImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        bImageFromRgb(bImage, red, green, blue, imageHeight, imageWidth);
        return bImage;
    }

    public ImagePlus createImageFromComponent(Component component) {
        updateMaps();
        BufferedImage bImage = null;
        if (component == Component.Y || component == Component.Cb || component == Component.Cr) {
            Matrix x = notRgbComponents.get(component);
            int width = x.getColumnDimension();
            int height = x.getRowDimension();
            bImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    int color = (int) x.get(i, j);
                    if (color < 0) {
                        color = Math.abs(color);
                    }
                    bImage.setRGB(j, i, new Color(color, color, color).getRGB());
                }
            }
        } else if (component == Component.BLUE || component == Component.GREEN || component == Component.RED) {
            bImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
            int[][] x = RgbComponents.get(component);
            bImageFromRgb(bImage, x, x, x, imageHeight, imageWidth);
        }
        return new ImagePlus(component.toString(), bImage);
    }

    public void convertRgbToYCbCr() {
        int r, b, g;
        for (int i = 0; i < imageHeight; i++) {
            for (int j = 0; j < imageWidth; j++) {
                r = red[i][j];
                b = blue[i][j];
                g = green[i][j];
                y.set(i, j, 0.257 * r + 0.504 * g + 0.098 * b + 16);
                Cb.set(i, j, -0.148 * r - 0.291 * g + 0.439 * b + 128);
                Cr.set(i, j, 0.439 * r - 0.368 * g - 0.071 * b + 128);
            }
        }
    }

    public void convertYCbCrToRgb() {
        double thisY, thisCB, thisCR;
        for (int i = 0; i < imageHeight; i++) {
            for (int j = 0; j < imageWidth; j++) {
                thisY = y.get(i, j);
                thisCB = Cb.get(i, j);
                thisCR = Cr.get(i, j);
                red[i][j] = fixValue(1.164 * (thisY - 16) + 1.596 * (thisCR - 128));
                green[i][j] = fixValue((1.164 * (thisY - 16)) - (0.813 * (thisCR - 128)) - (0.391 * (thisCB - 128)));
                blue[i][j] = fixValue(1.164 * (thisY - 16) + 2.018 * (thisCB - 128));
            }
        }
    }

    public Matrix downSample(Matrix matrix) {
        int height = matrix.getRowDimension();
        int width = matrix.getColumnDimension();
        Matrix downSampled = new Matrix(height, width / 2);
        for (int i = 0; i < width; i += 2) {
            Matrix subMatrix = matrix.getMatrix(0, height - 1, i, i);
            downSampled.setMatrix(0, height - 1, i / 2, i / 2, subMatrix);
        }
        return downSampled;
    }

    public Matrix upSample(Matrix matrix) {
        int height = matrix.getRowDimension();
        int width = matrix.getColumnDimension();
        Matrix upSampled = new Matrix(height, width * 2);
        for (int i = 0; i < width; i++) {
            Matrix subMatrix = matrix.getMatrix(0, height - 1, i, i);
            upSampled.setMatrix(0, height - 1, i * 2, i * 2, subMatrix);
            upSampled.setMatrix(0, height - 1, (i * 2) + 1, (i * 2) + 1, subMatrix);
        }
        return upSampled;
    }

    public Matrix transform(Matrix transformMatrix, Matrix inputMatrix) {
        return transformMatrix.times(inputMatrix).times(transformMatrix.transpose());
    }

    public Matrix inverseTransform(Matrix transformMatrix, Matrix inputMatrix) {
        return transformMatrix.transpose().times(inputMatrix).times(transformMatrix);
    }

    public void divideComponentIntoBlocks(Matrix matrix, int size, Consumer<Matrix[]> setter) {
        int widthBlocks = matrix.getColumnDimension() / size;
        int heightBlocks = matrix.getRowDimension() / size;
        Matrix[] blocks = new Matrix[widthBlocks * heightBlocks];
        int counter = 0;
        for (int i = 0; i < heightBlocks; i++) {
            for (int j = 0; j < widthBlocks; j++) {
                Matrix temp = matrix.getMatrix(i * size, i * size + (size - 1), j * size, j * size + (size - 1));
                blocks[counter] = new Matrix(size, size);
                blocks[counter].setMatrix(0, size - 1, 0, size - 1, temp);
                counter++;
            }
        }
        setter.accept(blocks);
    }

    public void mergeBlocksIntoComponent(int size, Matrix componentMatrix, Matrix[] blocks) {
        int height = componentMatrix.getRowDimension();
        int width = componentMatrix.getColumnDimension();
        int counter = 0;
        for (int i = 0; i < height / size; i++) {
            for (int j = 0; j < width / size; j++) {
                componentMatrix.setMatrix(i * size, i * size + (size - 1), j * size, j * size + (size - 1), blocks[counter]);
                counter++;
            }
        }
    }

    public void transformBlocks(Matrix transformMatrix, Matrix[] blocks, BiFunction<Matrix, Matrix, Matrix> transformation) {
        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = transformation.apply(transformMatrix, blocks[i]);
        }
    }

    public void quantize(int[][] quantizationMatrix, Matrix[] blocks, BiFunction<Double, Integer, Integer> function) {
        for (Matrix block : blocks) {
            for (int j = 0; j < 8; j++) {
                for (int k = 0; k < 8; k++) {
                    int value = function.apply(block.get(j, k), quantizationMatrix[j][k]);
                    block.set(j, k, value);
                }
            }
        }
    }

    private int fixValue(double value) {
        int newValue = (int) Math.round(value);
        if (newValue > 255) {
            return 255;
        } else if (newValue < 0) {
            return 0;
        }
        return newValue;
    }

    public static void bImageFromRgb(BufferedImage bImage, int[][] red, int[][] green, int[][] blue, int imageHeight, int imageWidth) {
        for (int i = 0; i < imageHeight; i++) {
            for (int j = 0; j < imageWidth; j++) {
                int rgb = new Color(red[i][j], green[i][j], blue[i][j]).getRGB();
                bImage.setRGB(j, i, rgb);
            }
        }
    }


    public void setRed(int[][] red) {
        this.red = red;
    }

    public void setGreen(int[][] green) {
        this.green = green;
    }

    public void setBlue(int[][] blue) {
        this.blue = blue;
    }

    public Matrix getY() {
        return y;
    }

    public Matrix getCb() {
        return Cb;
    }

    public Matrix getCr() {
        return Cr;
    }

    public void setCb(Matrix cb) {
        this.Cb = cb;
    }

    public void setCr(Matrix cr) {
        this.Cr = cr;
    }

    public void setY(Matrix y) {
        this.y = y;
    }

    public int[][] getRed() {
        return red;
    }

    public int[][] getGreen() {
        return green;
    }

    public int[][] getBlue() {
        return blue;
    }

    public void setBlocksY(Matrix[] blocksY) {
        this.blocksY = blocksY;
    }

    public void setBlocksCb(Matrix[] blocksCb) {
        this.blocksCb = blocksCb;
    }

    public void setBlocksCr(Matrix[] blocksCr) {
        this.blocksCr = blocksCr;
    }

    public Matrix[] getBlocksY() {
        return blocksY;
    }

    public Matrix[] getBlocksCb() {
        return blocksCb;
    }

    public Matrix[] getBlocksCr() {
        return blocksCr;
    }

    public HashMap<Component, int[][]> getRgbComponents() {
        return RgbComponents;
    }

    public HashMap<Component, Consumer<int[][]>> getRgbSetters() {
        return RgbSetters;
    }

    public void setbImage(BufferedImage bImage) {
        this.bImage = bImage;
    }
}