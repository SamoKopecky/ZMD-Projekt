package main;

import Jama.Matrix;
import ij.ImagePlus;
import main.enums.Component;
import main.enums.Sampler;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import java.util.function.Function;

public class Process {
    private ColorTransform cTrans;
    private ColorTransform colorTransformOriginal;
    private final ImagePlus imagePlus;
    private Watermark watermark;

    public Process(ImagePlus imagePlus) {
        this.imagePlus = imagePlus;
        loadOriginalImage();
        //imagePlus.show();
    }

    public ImagePlus getComponent(Component component) {
        return cTrans.createImageFromComponent(component);
    }

    public void loadOriginalImage() {
        colorTransformOriginal = new ColorTransform(imagePlus.getBufferedImage());
        colorTransformOriginal.fromImageToRgb();
        colorTransformOriginal.convertRgbToYCbCr();
        cTrans = new ColorTransform(imagePlus.getBufferedImage());
        cTrans.fromImageToRgb();
        cTrans.convertRgbToYCbCr();
    }

    public void showImage(String name) {
        cTrans.convertYCbCrToRgb();
        cTrans.createImageFromRgb(name).show();
    }

    public void downSample(Sampler sampler) {
        Matrix[] matrices = new Matrix[]{
                new Matrix(cTrans.getCb().getArray()),
                new Matrix(cTrans.getCr().getArray())
        };
        if (sampler == Sampler.S444) {
            loadOriginalImage();
            return;
        }
        useSample(sampler, matrices, cTrans::downSample);
    }

    public void upSample(Sampler sampler) {
        Matrix[] matrices = new Matrix[]{
                new Matrix(cTrans.getCb().getArray()),
                new Matrix(cTrans.getCr().getArray())
        };
        if (sampler == Sampler.S444) {
            loadOriginalImage();
            return;
        }
        useSample(sampler, matrices, cTrans::upSample);
    }

    private void useSample(Sampler sampler, Matrix[] matrices, Function<Matrix, Matrix> matrixFunc) {
        changeMatrices(matrixFunc, matrices);
        switch (sampler) {
            case S411:
                changeMatrices(matrixFunc, matrices);
                break;
            case S420:
                changeMatrices(Matrix::transpose, matrices);
                changeMatrices(matrixFunc, matrices);
                changeMatrices(Matrix::transpose, matrices);
        }
        cTrans.setCb(matrices[0]);
        cTrans.setCr(matrices[1]);
    }

    private void changeMatrices(Function<Matrix, Matrix> change, Matrix[] matrices) {
        matrices[0] = change.apply(matrices[0]);
        matrices[1] = change.apply(matrices[1]);
    }

    public void divideIntoBlocks(int size) {
        cTrans.divideComponentIntoBlocks(cTrans.getY(), size, cTrans::setBlocksY);
        cTrans.divideComponentIntoBlocks(cTrans.getCb(), size, cTrans::setBlocksCb);
        cTrans.divideComponentIntoBlocks(cTrans.getCr(), size, cTrans::setBlocksCr);
    }

    public void mergeBlocksIntoComponent(int size) {
        cTrans.mergeBlocksIntoComponent(size, cTrans.getY(), cTrans.getBlocksY());
        cTrans.mergeBlocksIntoComponent(size, cTrans.getCb(), cTrans.getBlocksCb());
        cTrans.mergeBlocksIntoComponent(size, cTrans.getCr(), cTrans.getBlocksCr());
    }

    public void transformBlocks(Matrix transformMatrix) {
        cTrans.transformBlocks(transformMatrix, cTrans.getBlocksY(), cTrans::transform);
        cTrans.transformBlocks(transformMatrix, cTrans.getBlocksCb(), cTrans::transform);
        cTrans.transformBlocks(transformMatrix, cTrans.getBlocksCr(), cTrans::transform);
    }

    public void inverseBlocks(Matrix transformMatrix) {
        cTrans.transformBlocks(transformMatrix, cTrans.getBlocksY(), cTrans::inverseTransform);
        cTrans.transformBlocks(transformMatrix, cTrans.getBlocksCb(), cTrans::inverseTransform);
        cTrans.transformBlocks(transformMatrix, cTrans.getBlocksCr(), cTrans::inverseTransform);
    }


    public void sample(Sampler sampleType, Consumer<Sampler> sampleFunc) {
        sampleFunc.accept(sampleType);
    }

    public void quantize(int q) {
        Quantization quantization = new Quantization();
        quantization.scale(q);
        cTrans.quantize(quantization.matrixY, cTrans.getBlocksY(), (d, i) -> (int) (d / i));
        cTrans.quantize(quantization.matrixColor, cTrans.getBlocksCr(), (d, i) -> (int) (d / i));
        cTrans.quantize(quantization.matrixColor, cTrans.getBlocksCb(), (d, i) -> (int) (d / i));
    }

    public void inverseQuantize(int q) {
        Quantization quantization = new Quantization();
        quantization.scale(q);
        cTrans.quantize(quantization.matrixY, cTrans.getBlocksY(), (d, i) -> (int) (d * i));
        cTrans.quantize(quantization.matrixColor, cTrans.getBlocksCr(), (d, i) -> (int) (d * i));
        cTrans.quantize(quantization.matrixColor, cTrans.getBlocksCb(), (d, i) -> (int) (d * i));
    }

    public void putLsbWatermark(int bitDepth, Component component, Function<BufferedImage, BufferedImage> attackFunc,
                                int compression) {
        watermark = new Watermark(bitDepth);
        BufferedImage bufferedImage = watermark.putLsbWatermark(component);
        Attack.compression = compression;
        watermark.cTrans.setbImage(attackFunc.apply(bufferedImage));
        watermark.cTrans.fromImageToRgb();
    }

    public ImagePlus extractLsbWatermark(Component component) {
        return watermark.extractLsbWatermark(component);
    }

    public void putTranWatermark(int u1, int v1, int u2, int v2, Function<Integer, Matrix> function, int bitDepth,
                                 Function<BufferedImage, BufferedImage> attackFunc, int compression) {
        watermark = new Watermark(bitDepth);
        watermark.putTranWatermark(function, u1, v1, u2, v2);
        Attack.compression = compression;
        watermark.cTrans.setbImage(attackFunc.apply(watermark.cTrans.createBufferedImageFromRgb()));
        watermark.cTrans.fromImageToRgb();
        watermark.cTrans.convertRgbToYCbCr();
    }

    public ImagePlus extractTranWatermark(int u1, int v1, int u2, int v2, Function<Integer, Matrix> function) {
        return watermark.extractTranWatermark(function, u1, v1, u2, v2);
    }


    public String[] calculateQuality(ColorTransform original, ColorTransform edited) {
        String[] result = new String[2];
        Quality quality = new Quality();
        double redMse = quality.getMse(original.getRed(), edited.getRed());
        double blueMse = quality.getMse(original.getBlue(), edited.getBlue());
        double greenMse = quality.getMse(original.getGreen(), edited.getGreen());
        double mse = (redMse + blueMse + greenMse) / 3;
        result[0] = String.format("%.2f", mse);
        result[1] = String.format("%.2f dB", quality.getPsnr(mse));
        return result;
    }

    public ColorTransform getcTrans() {
        return cTrans;
    }

    public ColorTransform getColorTransformOriginal() {
        return colorTransformOriginal;
    }

    public Watermark getWatermark() {
        return watermark;
    }
}