package main;

public class Quality {
    public double getMse(int[][] original, int[][] edited) {
        double mse = 0;
        int height = original.length;
        int width = original[0].length;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                mse += Math.pow(original[i][j] - edited[i][j], 2);
            }
        }
        mse = mse / (height * width);
        return mse;

    }

    public double getPsnr(double mse) {
        System.out.println("a");
        double log_arg = Math.pow((Math.pow(2, 24) - 1), 2) / mse;
        return 10 * Math.log10(log_arg);
    }
}
