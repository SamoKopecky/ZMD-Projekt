package main;

import Jama.Matrix;
import ij.ImagePlus;
import main.enums.Component;
import main.enums.Sampler;

import javax.swing.*;
import java.awt.*;
import java.util.Iterator;
import java.util.function.Function;

public class MainWindow {
    private Process process;
    private JPanel mainPanel;
    private JButton redButton;
    private JButton greenButton;
    private JButton blueButton;
    private JButton yButton;
    private JButton cbButton;
    private JButton crButton;
    private JButton qualityButton;
    private JTextField mseField;
    private JTextField psnrField;
    private JTextField blockSize;
    private JLabel BlockSizeLabel;
    private JButton startProcessButton;
    private JTextField transformation;
    private JLabel TLabel;
    private JRadioButton a422RadioButton;
    private JRadioButton a411RadioButton;
    private JRadioButton a420RadioButton;
    private JRadioButton a2DDCTRadioButton;
    private JRadioButton a2DWHTRadioButton;
    private JSlider qSlider;
    private JTextField qValue;
    private JButton resetButton;
    private JButton RGBButton;
    private final ButtonGroup transGroup = new ButtonGroup();
    private final ButtonGroup scaleGroup = new ButtonGroup();
    private Function<Integer, Matrix> transformationFunc;
    private Sampler sampler;
    private int q;

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
        blockSize.setText("8");
        qValue.setText("50");
        transGroup.add(a2DDCTRadioButton);
        transGroup.add(a2DWHTRadioButton);
        scaleGroup.add(a411RadioButton);
        scaleGroup.add(a420RadioButton);
        scaleGroup.add(a422RadioButton);
        a420RadioButton.setSelected(true);
        a2DDCTRadioButton.setSelected(true);
        redButton.addActionListener(e -> process.getComponent(Component.RED).show());
        blueButton.addActionListener(e -> process.getComponent(Component.BLUE).show());
        greenButton.addActionListener(e -> process.getComponent(Component.GREEN).show());
        yButton.addActionListener(e -> process.getComponent(Component.Y).show());
        cbButton.addActionListener(e -> process.getComponent(Component.Cb).show());
        crButton.addActionListener(e -> process.getComponent(Component.Cr).show());
        qualityButton.addActionListener(e -> calculate(process.getColorTransformOriginal(), process.getColorTransform()));
        startProcessButton.addActionListener(e -> transform(Integer.parseInt(blockSize.getText())));
        qSlider.addChangeListener(e -> {
            q = qSlider.getValue();
            qValue.setText(Integer.toString(q));
        });
        resetButton.addActionListener(e -> reset());
        RGBButton.addActionListener(e -> process.showImage());
    }

    private void reset() {
        Window[] windows = Window.getWindows();
        for (int i = 1; i < windows.length; i++) {
            windows[i].dispose();
        }
        process.loadOriginalImage();
    }

    private void transform(int size) {
        process.loadOriginalImage();
        chooseSettings();
        process.sample(sampler, process::downSample);
        process.divideIntoBlocks(size);
        Matrix transformMatrix = transformationFunc.apply(size);
        process.transformBlocks(transformMatrix);
        process.quantize(q);
        process.inverseQuantize(q);
        process.inverseBlocks(transformMatrix);
        process.mergeBlocksIntoComponent(size);
        process.sample(sampler, process::upSample);
        process.showImage();
    }

    private void chooseSettings() {
        q = qSlider.getValue();
        for (Iterator<AbstractButton> it = transGroup.getElements().asIterator(); it.hasNext(); ) {
            AbstractButton button = it.next();
            if (button.isSelected()) {
                String setting = button.getText();
                switch (setting){
                    case "2D-DCT":
                        transformationFunc = TransformationMatrix::getDctMatrix;
                        break;
                    case "2D-WHT":
                        transformationFunc = TransformationMatrix::getWhtMatrix;
                        break;
                }
                break;
            }
        }
        for (Iterator<AbstractButton> it = scaleGroup.getElements().asIterator(); it.hasNext(); ) {
            AbstractButton button = it.next();
            if (button.isSelected()) {
                String setting = button.getText();
                switch (setting){
                    case "4:2:0":
                        sampler = Sampler.S420;
                        break;
                    case "4:2:2":
                        sampler = Sampler.S422;
                        break;
                    case "4:1:1":
                        sampler = Sampler.S411;
                        break;
                }
                break;
            }
        }
    }

    private void calculate(ColorTransform original, ColorTransform edited) {
        Quality quality = new Quality();
        edited.convertYCbCrToRgb();
        double redMse = quality.getMse(original.getRed(), edited.getRed());
        double blueMse = quality.getMse(original.getBlue(), edited.getBlue());
        double greenMse = quality.getMse(original.getGreen(), edited.getGreen());
        double mse = (redMse + blueMse + greenMse) / 3;
        mseField.setText(String.format("%.2f", mse));
        psnrField.setText(String.format("%.2f dB", quality.getPsnr(mse)));

    }

    private void Initialize() {
        process = new Process(new ImagePlus("Lenna.png"));
    }
}
