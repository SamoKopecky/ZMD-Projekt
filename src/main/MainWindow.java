package main;

import Jama.Matrix;
import ij.ImagePlus;
import main.enums.Component;
import main.enums.Sampler;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
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
    private JButton tranExtractWatermarkButton;
    private JButton tranPutWatermarkButton;
    private JTextField u1;
    private JTextField v1;
    private JTextField u2;
    private JTextField v2;
    private JTextField insertDepthLabel;
    private JRadioButton tranDCTRadioButton;
    private JRadioButton tranWHTRadioButton;
    private JRadioButton rotate90RadioButton;
    private JRadioButton noneRadioButton;
    private JRadioButton JPEGRadioButton;
    private JTextField attackJpegQualityTextField;
    private JSlider attackJpegQualityJSlider;
    private JRadioButton rotate45RadioButton;
    private JButton attackQualityButton;
    private JTextField attackMSE;
    private JTextField attackPSNR;
    private final ButtonGroup transGroup = new ButtonGroup();
    private final ButtonGroup scaleGroup = new ButtonGroup();
    private final ButtonGroup watermarkGroup = new ButtonGroup();
    private final ButtonGroup tranWatermarkGroup = new ButtonGroup();
    private final ButtonGroup attackGroup = new ButtonGroup();

    private Function<Integer, Matrix> transformationFunc;
    private Function<Integer, Matrix> tranWatermarkTranFunc;
    private Function<BufferedImage, BufferedImage> attackFunc;
    private Sampler sampler;
    private int q;
    private int bitDepth;
    private int jpegQuality;
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
        tranWatermarkGroup.add(tranDCTRadioButton);
        tranWatermarkGroup.add(tranWHTRadioButton);
        attackGroup.add(rotate90RadioButton);
        attackGroup.add(rotate45RadioButton);
        attackGroup.add(JPEGRadioButton);
        attackGroup.add(noneRadioButton);

        a420RadioButton.setSelected(true);
        a2DDCTRadioButton.setSelected(true);
        redBits.setSelected(true);
        tranDCTRadioButton.setSelected(true);
        noneRadioButton.setSelected(true);

        redButton.addActionListener(e -> process.getComponent(Component.RED).show());
        blueButton.addActionListener(e -> process.getComponent(Component.BLUE).show());
        greenButton.addActionListener(e -> process.getComponent(Component.GREEN).show());
        yButton.addActionListener(e -> process.getComponent(Component.Y).show());
        cbButton.addActionListener(e -> process.getComponent(Component.Cb).show());
        crButton.addActionListener(e -> process.getComponent(Component.Cr).show());
        qualityButton.addActionListener(e -> {
            String[] result = process.calculateQuality(process.getColorTransformOriginal(), process.getcTrans());
            mseField.setText(result[0]);
            psnrField.setText(result[1]);
        });
        attackQualityButton.addActionListener(e -> {
            String[] result = process.calculateQuality(process.getWatermark().originalWatermark,
                    process.getWatermark().extractedWatermark);
            attackMSE.setText(result[0]);
            attackPSNR.setText(result[1]);
        });
        startProcessButton.addActionListener(e -> transform(Integer.parseInt(blockSize.getText())));
        qSlider.addChangeListener(e -> {
            q = qSlider.getValue();
            qValue.setText(Integer.toString(q));
        });
        qValue.addActionListener(e -> qSlider.setValue(Integer.parseInt(qValue.getText())));
        attackJpegQualityJSlider.addChangeListener(e -> {
            jpegQuality = attackJpegQualityJSlider.getValue();
            attackJpegQualityTextField.setText(Integer.toString(jpegQuality));
        });
        attackJpegQualityTextField.addActionListener(e -> attackJpegQualityJSlider.setValue(Integer.parseInt(attackJpegQualityTextField.getText())));
        resetButton.addActionListener(e -> process.loadOriginalImage());
        RGBButton.addActionListener(e -> process.showImage("RGB"));
        closeAllButton.addActionListener(e -> reset());
        bitDepthSlider.addChangeListener(e -> {
            bitDepth = bitDepthSlider.getValue();
            bitDepthTextField.setText(Integer.toString(bitDepth));
        });
        bitDepthTextField.addActionListener(e -> bitDepthSlider.setValue(Integer.parseInt(bitDepthTextField.getText())));
        putWatermarkButton.addActionListener(e -> {
            chooseSettings();
            process.insertSpaceWatermark(bitDepth, watermarkComp, attackFunc, jpegQuality);
        });
        extractWatermarkButton.addActionListener(e -> {
            chooseSettings();
            process.extractSpaceWatermark(watermarkComp).show();
        });
        tranPutWatermarkButton.addActionListener(e -> {
            chooseSettings();
            process.putTranWatermark(
                    Integer.parseInt(u1.getText()),
                    Integer.parseInt(v1.getText()),
                    Integer.parseInt(u2.getText()),
                    Integer.parseInt(v2.getText()),
                    tranWatermarkTranFunc,
                    Integer.parseInt(insertDepthLabel.getText()),
                    attackFunc, jpegQuality
            );
        });
        tranExtractWatermarkButton.addActionListener(e -> {
            chooseSettings();
            process.extractTranWatermark(
                    Integer.parseInt(u1.getText()),
                    Integer.parseInt(v1.getText()),
                    Integer.parseInt(u2.getText()),
                    Integer.parseInt(v2.getText()),
                    tranWatermarkTranFunc
            ).show();
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
        process.showImage("RGB");
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

        HashMap<String, Function<BufferedImage, BufferedImage>> attack = new HashMap<>();
        attack.put("Rotate 90°", Attack::rotateImage90);
        attack.put("Rotate 45°", Attack::rotateImage45);
        attack.put("JPEG compression", Attack::jpegCompression);
        attack.put("None", image -> image);

        q = qSlider.getValue();
        jpegQuality = attackJpegQualityJSlider.getValue();

        transformationFunc = setSelected(transGroup, transform);
        tranWatermarkTranFunc = setSelected(tranWatermarkGroup, transform);
        watermarkComp = setSelected(watermarkGroup, watermark);
        sampler = setSelected(scaleGroup, sampling);
        attackFunc = setSelected(attackGroup, attack);
    }

    private <T> T setSelected(ButtonGroup buttonGroup, HashMap<String, T> map) {
        for (Iterator<AbstractButton> it = buttonGroup.getElements().asIterator(); it.hasNext(); ) {
            AbstractButton button = it.next();
            if (button.isSelected()) {
                return map.get(button.getText());
            }
        }
        return null;
    }

    private void Initialize() {
        process = new Process(new ImagePlus("Lenna.png"));
    }


}
