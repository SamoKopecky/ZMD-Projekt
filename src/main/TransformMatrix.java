package main;

import Jama.Matrix;

public class TransformMatrix {
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
}
