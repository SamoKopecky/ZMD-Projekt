package main;

import Jama.Matrix;
import ij.ImagePlus;
import main.enums.Component;
import main.enums.Sampler;

import java.util.function.Consumer;
import java.util.function.Function;

public class Process {
    private ColorTransform colorTransform;
    private ColorTransform colorTransformOriginal;
    private final ImagePlus imagePlus;

    public Process(ImagePlus imagePlus) {
        this.imagePlus = imagePlus;
        loadOriginalImage();
        imagePlus.show();
    }

    public ImagePlus getComponent(Component component) {
        return colorTransform.createImageFromComponent(component);
    }

    public void loadOriginalImage() {
        colorTransformOriginal = new ColorTransform(imagePlus.getBufferedImage());
        colorTransformOriginal.fromImageToRgb();
        colorTransformOriginal.convertRgbToYCbCr();
        colorTransform = new ColorTransform(imagePlus.getBufferedImage());
        colorTransform.fromImageToRgb();
        colorTransform.convertRgbToYCbCr();
    }

    public void showImage() {
        colorTransform.convertYCbCrToRgb();
        colorTransform.createImageFromRgb().show();
    }

    public void downSample(Sampler sampler) {
        Matrix[] matrices = new Matrix[]{
                new Matrix(colorTransform.getCb().getArray()),
                new Matrix(colorTransform.getCr().getArray())
        };
        if (sampler == Sampler.S444) {
            loadOriginalImage();
            return;
        }
        useSample(sampler, matrices, colorTransform::downSample);
    }

    public void upSample(Sampler sampler) {
        Matrix[] matrices = new Matrix[]{
                new Matrix(colorTransform.getCb().getArray()),
                new Matrix(colorTransform.getCr().getArray())
        };
        if (sampler == Sampler.S444) {
            loadOriginalImage();
            return;
        }
        useSample(sampler, matrices, colorTransform::upSample);
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
        colorTransform.setCb(matrices[0]);
        colorTransform.setCr(matrices[1]);
    }

    private void changeMatrices(Function<Matrix, Matrix> change, Matrix[] matrices) {
        matrices[0] = change.apply(matrices[0]);
        matrices[1] = change.apply(matrices[1]);
    }

    public void divideIntoBlocks(int size) {
        colorTransform.divideComponentIntoBlocks(colorTransform.getY(), size, colorTransform::setBlocksY);
        colorTransform.divideComponentIntoBlocks(colorTransform.getCb(), size, colorTransform::setBlocksCb);
        colorTransform.divideComponentIntoBlocks(colorTransform.getCr(), size, colorTransform::setBlocksCr);
    }

    public void mergeBlocksIntoComponent(int size) {
        colorTransform.mergeBlocksIntoComponent(size, colorTransform.getY(), colorTransform.getBlocksY());
        colorTransform.mergeBlocksIntoComponent(size, colorTransform.getCb(), colorTransform.getBlocksCb());
        colorTransform.mergeBlocksIntoComponent(size, colorTransform.getCr(), colorTransform.getBlocksCr());
    }

    public void transformBlocks(Matrix transformMatrix) {
        colorTransform.transformBlocks(transformMatrix, colorTransform.getBlocksY());
        colorTransform.transformBlocks(transformMatrix, colorTransform.getBlocksCb());
        colorTransform.transformBlocks(transformMatrix, colorTransform.getBlocksCr());
    }

    public void inverseBlocks(Matrix transformMatrix){
        colorTransform.inverseTransformBlocks(transformMatrix, colorTransform.getBlocksY());
        colorTransform.inverseTransformBlocks(transformMatrix, colorTransform.getBlocksCb());
        colorTransform.inverseTransformBlocks(transformMatrix, colorTransform.getBlocksCr());
    }


    public void sample(Sampler sampleType, Consumer<Sampler> sampleFunc) {
        sampleFunc.accept(sampleType);
    }

    public void quantize(int q) {
        Quantization quantization = new Quantization();
        quantization.scale(q);
        colorTransform.quantize(quantization.matrixY, colorTransform.getBlocksY());
        colorTransform.quantize(quantization.matrixColor, colorTransform.getBlocksCr());
        colorTransform.quantize(quantization.matrixColor, colorTransform.getBlocksCb());
    }

    public void inverseQuantize(int q) {
        Quantization quantization = new Quantization();
        quantization.scale(q);
        colorTransform.inverseQuantize(quantization.matrixY, colorTransform.getBlocksY());
        colorTransform.inverseQuantize(quantization.matrixColor, colorTransform.getBlocksCr());
        colorTransform.inverseQuantize(quantization.matrixColor, colorTransform.getBlocksCb());
    }



    public ColorTransform getColorTransform() {
        return colorTransform;
    }

    public ColorTransform getColorTransformOriginal() {
        return colorTransformOriginal;
    }
}
