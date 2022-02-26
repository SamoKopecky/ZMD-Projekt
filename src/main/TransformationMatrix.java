package main;

import Jama.Matrix;

public class TransformationMatrix {
    public static Matrix getDctMatrix(int size) {
        Matrix dctMatrix = new Matrix(size, size);
        for (int j = 0; j < size; j++) {
            dctMatrix.set(0, j, Math.sqrt((double) 1 / size) * Math.cos(0));
        }

        for (int i = 1; i < size; i++) {
            for (int j = 0; j < size; j++) {
                double numerator = (2 * j + 1) * i * Math.PI;
                double cos = Math.cos(numerator / (2 * size));
                double value = Math.sqrt((double) 2 / size) * cos;
                dctMatrix.set(i, j, value);
            }
        }
        return dctMatrix;
    }

    public static Matrix getWhtMatrix(int size) {
        Matrix result = null;
        Matrix lastMatrix = new Matrix(1, 1);
        lastMatrix.set(0, 0, 1);
        int stepSize = 0;
        int rounds = (int) (Math.log(size) / Math.log(2));
        for (int i = 1; i <= rounds; i++) {
            stepSize = (int) Math.pow(2, i);
            result = new Matrix(stepSize, stepSize);
            int full = stepSize - 1;
            int half = (stepSize / 2) - 1;
            result.setMatrix(0, half, 0, half, lastMatrix);
            result.setMatrix(0, half, half + 1, full, lastMatrix);
            result.setMatrix(half + 1, full, 0, half, lastMatrix);
            result.setMatrix(half + 1, full, half + 1, full, lastMatrix.times(-1));
            lastMatrix = result;
        }

        return result.times(1.0 / Math.sqrt(stepSize));
    }
}
