package main;

import java.util.function.Function;

public class Quantization {
    public int[][] matrixY = {
            {16, 11, 10, 16, 24, 40, 51, 61},
            {12, 12, 14, 19, 26, 58, 60, 55},
            {14, 13, 16, 24, 40, 57, 69, 56},
            {14, 17, 22, 29, 51, 87, 80, 62},
            {18, 22, 37, 56, 68, 109, 103, 77},
            {24, 35, 55, 64, 81, 104, 113, 92},
            {49, 64, 78, 87, 103, 121, 120, 101},
            {72, 92, 95, 98, 112, 100, 103, 99}
    };
    public int[][] matrixColor = {
            {17, 18, 24, 47, 99, 99, 99, 99},
            {18, 21, 26, 66, 99, 99, 99, 99},
            {24, 26, 56, 99, 99, 99, 99, 99},
            {47, 66, 99, 99, 99, 99, 99, 99},
            {99, 99, 99, 99, 99, 99, 99, 99},
            {99, 99, 99, 99, 99, 99, 99, 99},
            {99, 99, 99, 99, 99, 99, 99, 99},
            {99, 99, 99, 99, 99, 99, 99, 99}
    };

    public void scale(int q) {
        Function<Integer, Double> scaleFunc;
        if (q <= 50) {
            scaleFunc = (qNum) -> (double) (50.0 / qNum);
        } else if (q == 100) {
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                   matrixY[i][j] = 1;
                   matrixColor[i][j] = 1;
                }
            }
            return;
        } else {
            scaleFunc = (qNum) -> (double) (2 - (2 * qNum / 100.0));
        }
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                double adjustion = scaleFunc.apply(q);
                matrixY[i][j] = (int) Math.ceil(adjustion * matrixY[i][j]);
                matrixColor[i][j] = (int) Math.ceil(adjustion * matrixColor[i][j]);
            }
        }
    }
}
