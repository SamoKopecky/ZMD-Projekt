package main;

import Jama.Matrix;
import ij.ImagePlus;
import main.org.enums.Component;
import main.org.enums.Sampler;

import javax.swing.*;
import java.util.function.Consumer;

public class MainWindow {
    private Process process;
    private JPanel mainPanel;
    private JButton redButton;
    private JButton greenButton;
    private JButton blueButton;
    private JButton yButton;
    private JButton cbButton;
    private JButton crButton;
    private JButton scale444Button;
    private JButton scale420Button;
    private JButton scale422Button;
    private JButton scale411Button;
    private JButton from420Button;
    private JButton from411Button;
    private JButton from422Button;
    private JButton qualityButton;
    private JTextField mseField;
    private JTextField psnrField;
    private JButton transformButton;

    public static void main(String[] args) {
        JFrame frame = new JFrame("Aplikace");
        frame.setBounds(100, 100, 450, 300);
        frame.setContentPane(new MainWindow().mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public MainWindow() {
        Initialize();
        redButton.addActionListener(e -> process.getComponent(Component.RED).show());
        blueButton.addActionListener(e -> process.getComponent(Component.BLUE).show());
        greenButton.addActionListener(e -> process.getComponent(Component.GREEN).show());
        yButton.addActionListener(e -> process.getComponent(Component.Y).show());
        cbButton.addActionListener(e -> process.getComponent(Component.Cb).show());
        crButton.addActionListener(e -> process.getComponent(Component.Cr).show());
        scale444Button.addActionListener(e -> sample(Sampler.S444, process::downSample));
        scale422Button.addActionListener(e -> sample(Sampler.S422, process::downSample));
        scale420Button.addActionListener(e -> sample(Sampler.S420, process::downSample));
        scale411Button.addActionListener(e -> sample(Sampler.S411, process::downSample));
        from420Button.addActionListener(e -> sample(Sampler.S420, process::upSample));
        from411Button.addActionListener(e -> sample(Sampler.S411, process::upSample));
        from422Button.addActionListener(e -> sample(Sampler.S422, process::upSample));
        qualityButton.addActionListener(e -> calculate(process.getColorTransformOriginal().getPixels(),
                process.getColorTransform().getPixels()));
        transformButton.addActionListener(e -> transform());
    }

    private void transform() {
        //Matrix test = TransformMatrix.getDctMatrix(8);
        ColorTransform colTrans = process.getColorTransform();

        transformComponent(colTrans, colTrans::setY, colTrans.getY());
        transformComponent(colTrans, colTrans::setCb, colTrans.getCb());
        transformComponent(colTrans, colTrans::setCr, colTrans.getCr());

        colTrans.convertYCbCrToRgb();
        colTrans.createImageFromRgb().show();
    }

    private void transformComponent(ColorTransform colTrans, Consumer<Matrix> setter, Matrix matrix) {
        Matrix dctMatrix = TransformMatrix.getDctMatrix(matrix.getRowDimension());
        Matrix DctTransformed = colTrans.transform(dctMatrix, matrix);
        setter.accept(colTrans.inverseTransform(dctMatrix, DctTransformed));
    }

    private void sample(Sampler sampleType, Consumer<Sampler> sampleFunc) {
        sampleFunc.accept(sampleType);
        process.getComponent(Component.Y).show();
        process.getComponent(Component.Cr).show();
        process.getComponent(Component.Cb).show();
    }

    private void calculate(int[][] original, int[][] edited) {
        Quality quality = new Quality();
        double mse = quality.getMse(original, edited);
        mseField.setText(Double.toString(mse));
        psnrField.setText(String.format("%.2f", quality.getPsnr(mse)));

    }

    private void Initialize() {
        process = new Process(new ImagePlus("Lenna.png"));
    }

}
