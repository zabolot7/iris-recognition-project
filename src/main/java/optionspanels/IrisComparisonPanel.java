package optionspanels;

import core.ImageProcessor;
import core.IrisRecognitionProcessor;
import core.OptionPanel;
import core.PhotoPanel;
import core.MenuBar;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class IrisComparisonPanel extends JPanel {
    private PhotoPanel photoPanel;
    private OptionPanel parentPanel;
    private OptionPanel.BoundaryMode boundaryMode;

    private int[][][] rawImage1;
    private int[][][] rawImage2;
    private IrisRecognitionProcessor.IrisTemplate template1;
    private IrisRecognitionProcessor.IrisTemplate template2;

    public IrisComparisonPanel(PhotoPanel photoPanel, OptionPanel parentPanel) {
        this.photoPanel = photoPanel;
        this.parentPanel = parentPanel;

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(30, 20, 0, 20));

        boundaryMode = parentPanel.getBoundaryMode();

        loadDefaultImages();

        buildUI();
    }

    private void loadDefaultImages() {
        try {
            BufferedImage img1 = ImageIO.read(new File("src/MMU-Iris-Database/1/left/aeval1.bmp"));
            BufferedImage img2 = ImageIO.read(new File("src/MMU-Iris-Database/1/left/aeval2.bmp"));

            rawImage1 = photoPanel.createImageMatrix(img1);
            rawImage2 = photoPanel.createImageMatrix(img2);

            photoPanel.setDualImageMatrices(rawImage1, rawImage2);

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not load default images.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buildUI() {
        JLabel titleLabel = new JLabel("Compare two irises:");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JButton choosePicturesBtn = new JButton("<html><center>0. Choose Pictures</center></html>");
        choosePicturesBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton calcCodesBtn = new JButton("<html><center>1. Calculate Iris Codes</center></html>");
        calcCodesBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton calcDistanceBtn = new JButton("<html><center>2. Calculate Hamming Distance<br>(regular)</center></html>");
        calcDistanceBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        calcDistanceBtn.setEnabled(false);

        JButton calcMinDistanceBtn = new JButton("<html><center>3. Calculate Hamming Distance<br>(with iris rotation)</center></html>");
        calcMinDistanceBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        calcMinDistanceBtn.setEnabled(false);

        choosePicturesBtn.addActionListener(e -> {
            File file1 = MenuBar.chooseImageFile(this, "Select First Iris Image");
            if (file1 == null) return;

            File file2 = MenuBar.chooseImageFile(this, "Select Second Iris Image");
            if (file2 == null) return;

            try {
                BufferedImage img1 = ImageIO.read(file1);
                BufferedImage img2 = ImageIO.read(file2);

                rawImage1 = photoPanel.createImageMatrix(img1);
                rawImage2 = photoPanel.createImageMatrix(img2);

                photoPanel.setDualImageMatrices(rawImage1, rawImage2);

                template1 = null;
                template2 = null;
                calcDistanceBtn.setEnabled(false);
                calcMinDistanceBtn.setEnabled(false);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error reading image file.", "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        calcCodesBtn.addActionListener(e -> {
            if (rawImage1 == null || rawImage2 == null) return;

            template1 = processImageToTemplate(rawImage1);
            template2 = processImageToTemplate(rawImage2);

            if (template1 != null && template2 != null) {
                int[][][] vis1 = IrisRecognitionProcessor.createVisualBarcode(template1);
                int[][][] vis2 = IrisRecognitionProcessor.createVisualBarcode(template2);

                int[][][] composite1 = IrisRecognitionProcessor.createCompositeMatrix(rawImage1, vis1);
                int[][][] composite2 = IrisRecognitionProcessor.createCompositeMatrix(rawImage2, vis2);

                photoPanel.setDualImageMatrices(composite1, composite2);
                calcDistanceBtn.setEnabled(true);
                calcMinDistanceBtn.setEnabled(true);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to extract codes.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        calcDistanceBtn.addActionListener(e -> {
            if (template1 == null || template2 == null) return;

            double distance = IrisRecognitionProcessor.calculateHammingDistance(template1, template2);

            displayComparisonResult(distance);
        });

        calcMinDistanceBtn.addActionListener(e -> {
            if (template1 == null || template2 == null) return;

            double distance = IrisRecognitionProcessor.calculateMinHammingDistance(template1, template2);

            displayComparisonResult(distance);
        });

        this.add(titleLabel);
        this.add(Box.createVerticalStrut(20));
        this.add(choosePicturesBtn);
        this.add(Box.createVerticalStrut(20));
        this.add(calcCodesBtn);
        this.add(Box.createVerticalStrut(20));
        this.add(calcDistanceBtn);
        this.add(Box.createVerticalStrut(20));
        this.add(calcMinDistanceBtn);
        this.add(Box.createVerticalGlue());
    }

    /**
     * Helper to display the results of comparison between 2 eyes.
     */
    private void displayComparisonResult(double distance) {
        String formattedDistance = String.format("%.3f", distance);

        String resultText;
        if (distance < 0.32) {
            resultText = "MATCH: These are likely the same eye.";
        } else {
            resultText = "NO MATCH: These are likely different eyes.";
        }

        JOptionPane.showMessageDialog(this,
                "Fractional Hamming Distance: " + formattedDistance + "\n\n" + resultText,
                "Comparison Result",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Helper to run an image through the entire extraction pipeline without updating the UI at each step.
     */
    private IrisRecognitionProcessor.IrisTemplate processImageToTemplate(int[][][] inputMatrix) {
        try {
            int[][][] gray = ImageProcessor.applyGrayscale(inputMatrix, GrayscalePanel.GrayscaleOptions.LUMINANCE);
            int[][][] bin = IrisRecognitionProcessor.applyPupilBinarization(gray);
            int[][][] morph = IrisRecognitionProcessor.applyPupilMorphology(bin, boundaryMode);

            int[] center = IrisRecognitionProcessor.calculateCenter(morph);
            int pRadius = IrisRecognitionProcessor.calculateRadius(morph, center);
            int iRadius = IrisRecognitionProcessor.calculateDaugmanIrisRadius(gray, center, pRadius);

            int[][][] unwrapped = IrisRecognitionProcessor.generateIrisRectangle(gray, center, pRadius, iRadius);

            return IrisRecognitionProcessor.extractIrisCode(unwrapped);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

}