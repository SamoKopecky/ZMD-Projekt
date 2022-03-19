package main;

import Jama.Matrix;
import ij.ImagePlus;
import main.enums.Component;
import main.enums.Sampler;

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
        imagePlus.show();
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

    public void showImage() {
        cTrans.convertYCbCrToRgb();
        cTrans.createImageFromRgb().show();
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

    public void putWatermark(int bitDepth, Component component) {
        cTrans.updateMaps();
        watermark = new Watermark(bitDepth);
        watermark.putWatermark(component);
    }

    public ImagePlus extractWatermark(Component component){
        return watermark.extractWatermark(component);
    }


    public ColorTransform getcTrans() {
        return cTrans;
    }

    public ColorTransform getColorTransformOriginal() {
        return colorTransformOriginal;
    }
}