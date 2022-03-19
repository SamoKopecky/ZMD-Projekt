package main;

import Jama.Matrix;
import ij.ImagePlus;
import main.enums.Component;
import main.enums.Sampler;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
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
    private JButton closeAllButton;
    private JLabel transformControl;
    private JLabel watermarkingControls;
    private JLabel QValue;
    private JRadioButton redBits;
    private JTextField bitDepthTextField;
    private JRadioButton greenBits;
    private JRadioButton blueBits;
    private JButton putWatermarkButton;
    private JButton extractWatermarkButton;
    private JSlider bitDepthSlider;
    private final ButtonGroup transGroup = new ButtonGroup();
    private final ButtonGroup scaleGroup = new ButtonGroup();
    private final ButtonGroup watermarkGroup = new ButtonGroup();

    private Function<Integer, Matrix> transformationFunc;
    private Sampler sampler;
    private int q;
    private int bitDepth;
    private Component watermarkComp;

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
        bitDepthTextField.setText("3");
        bitDepth = 3;
        q = 50;
        transGroup.add(a2DDCTRadioButton);
        transGroup.add(a2DWHTRadioButton);
        scaleGroup.add(a411RadioButton);
        scaleGroup.add(a420RadioButton);
        scaleGroup.add(a422RadioButton);
        watermarkGroup.add(redBits);
        watermarkGroup.add(greenBits);
        watermarkGroup.add(blueBits);
        a420RadioButton.setSelected(true);
        a2DDCTRadioButton.setSelected(true);
        redBits.setSelected(true);
        redButton.addActionListener(e -> process.getComponent(Component.RED).show());
        blueButton.addActionListener(e -> process.getComponent(Component.BLUE).show());
        greenButton.addActionListener(e -> process.getComponent(Component.GREEN).show());
        yButton.addActionListener(e -> process.getComponent(Component.Y).show());
        cbButton.addActionListener(e -> process.getComponent(Component.Cb).show());
        crButton.addActionListener(e -> process.getComponent(Component.Cr).show());
        qualityButton.addActionListener(e -> calculate(process.getColorTransformOriginal(), process.getcTrans()));
        startProcessButton.addActionListener(e -> transform(Integer.parseInt(blockSize.getText())));
        qSlider.addChangeListener(e -> {
            q = qSlider.getValue();
            qValue.setText(Integer.toString(q));
        });
        resetButton.addActionListener(e -> process.loadOriginalImage());
        RGBButton.addActionListener(e -> process.showImage());
        closeAllButton.addActionListener(e -> reset());
        putWatermarkButton.addActionListener(e -> {
            chooseSettings();
            process.putWatermark(bitDepth, watermarkComp);
        });
        bitDepthSlider.addChangeListener(e -> {
            bitDepth = bitDepthSlider.getValue();
            bitDepthTextField.setText(Integer.toString(bitDepth));
        });
        extractWatermarkButton.addActionListener(e -> {
            chooseSettings();
            process.extractWatermark(watermarkComp).show();
        });
    }

    private void reset() {
        Window[] windows = Window.getWindows();
        for (int i = 1; i < windows.length; i++) {
            windows[i].dispose();
        }
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
        HashMap<String, Function<Integer, Matrix>> transform = new HashMap<>();
        transform.put("2D-DCT", TransformationMatrix::getDctMatrix);
        transform.put("2D-WHT", TransformationMatrix::getWhtMatrix);

        HashMap<String, Component> watermark = new HashMap<>();
        watermark.put("Red", Component.RED);
        watermark.put("Blue", Component.BLUE);
        watermark.put("Green", Component.GREEN);

        HashMap<String, Sampler> sampling = new HashMap<>();
        sampling.put("4:2:0", Sampler.S420);
        sampling.put("4:2:2", Sampler.S422);
        sampling.put("4:1:1", Sampler.S411);

        q = qSlider.getValue();
        for (Iterator<AbstractButton> it = transGroup.getElements().asIterator(); it.hasNext(); ) {
            AbstractButton button = it.next();
            if (button.isSelected()) {
                transformationFunc = transform.get(button.getText());
                break;
            }
        }
        for (Iterator<AbstractButton> it = watermarkGroup.getElements().asIterator(); it.hasNext(); ) {
            AbstractButton button = it.next();
            if (button.isSelected()) {
                watermarkComp = watermark.get(button.getText());
                break;
            }
        }
        for (Iterator<AbstractButton> it = scaleGroup.getElements().asIterator(); it.hasNext(); ) {
            AbstractButton button = it.next();
            if (button.isSelected()) {
                sampler = sampling.get(button.getText());
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
