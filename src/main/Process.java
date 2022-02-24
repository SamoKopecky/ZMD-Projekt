package main;

import Jama.Matrix;
import ij.ImagePlus;
import main.enums.Component;
import main.enums.Sampler;
import java.util.function.Function;

public class Process {
    private ColorTransform colorTransform;
    private ColorTransform colorTransformOriginal;
    private final ImagePlus imagePlus;

    public Process(ImagePlus imagePlus) {
        this.imagePlus = imagePlus;
        loadOriginalImage(imagePlus);
        imagePlus.show();
    }

    public ImagePlus getComponent(Component component) {
        return colorTransform.createImageFromComponent(component);
    }

    private void loadOriginalImage(ImagePlus imagePlus) {
        colorTransformOriginal = new ColorTransform(imagePlus.getBufferedImage());
        colorTransformOriginal.convertRgbToYCbCr();
        colorTransform = new ColorTransform(imagePlus.getBufferedImage());
        colorTransform.convertRgbToYCbCr();
    }

    public void downSample(Sampler sampler) {
        Matrix[] matrices = new Matrix[]{
                new Matrix(colorTransform.getCb().getArray()),
                new Matrix(colorTransform.getCr().getArray())
        };
        if (sampler == Sampler.S444) {
            loadOriginalImage(imagePlus);
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
            loadOriginalImage(imagePlus);
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

    public ColorTransform getColorTransform() {
        return colorTransform;
    }

    public ColorTransform getColorTransformOriginal() {
        return colorTransformOriginal;
    }
}
