package optionspanels;

import core.ImageProcessor;
import core.IrisRecognitionProcessor;
import core.OptionPanel;
import core.PhotoPanel;

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
            BufferedImage img2 = ImageIO.read(new File("src/MMU-Iris-Database/1/left/aeval2.bmp")); // Provide a second valid path

            rawImage1 = convertToMatrix(img1);
            rawImage2 = convertToMatrix(img2);

            photoPanel.setDualImageMatrices(rawImage1, rawImage2);

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not load default images.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int[][][] convertToMatrix(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[][][] matrix = new int[h][w][3];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                matrix[y][x][0] = (rgb >> 16) & 0xFF;
                matrix[y][x][1] = (rgb >> 8) & 0xFF;
                matrix[y][x][2] = rgb & 0xFF;
            }
        }
        return matrix;
    }

    private void buildUI() {
        JLabel titleLabel = new JLabel("Compare two irises:");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JButton choosePicturesBtn = new JButton("1. Choose Pictures");
        choosePicturesBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton calcCodesBtn = new JButton("2. Calculate Iris Codes");
        calcCodesBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton calcDistanceBtn = new JButton("<html><center>3. Calculate Hamming Distance<br>(regular)</center></html>");
        calcDistanceBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        calcDistanceBtn.setEnabled(false);

        JButton calcMinDistanceBtn = new JButton("<html><center>4. Calculate Hamming Distance<br>(with iris rotation)</center></html>");
        calcMinDistanceBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        calcMinDistanceBtn.setEnabled(false);

        choosePicturesBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "File chooser will be implemented later.", "Info", JOptionPane.INFORMATION_MESSAGE);
        });

        calcCodesBtn.addActionListener(e -> {
            if (rawImage1 == null || rawImage2 == null) return;

            template1 = processImageToTemplate(rawImage1);
            template2 = processImageToTemplate(rawImage2);

            if (template1 != null && template2 != null) {
                int[][][] vis1 = createVisualBarcode(template1);
                int[][][] vis2 = createVisualBarcode(template2);

                int[][][] composite1 = createCompositeMatrix(rawImage1, vis1);
                int[][][] composite2 = createCompositeMatrix(rawImage2, vis2);

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

    /**
     * Converts the boolean arrays into a visual RGB matrix (White=1, Black=0, Gray=Masked).
     */
    private int[][][] createVisualBarcode(IrisRecognitionProcessor.IrisTemplate template) {
        int rows = template.code.length;
        int cols = template.code[0].length;
        int[][][] codeImage = new int[rows][cols][3];

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                if (!template.mask[y][x]) {
                    codeImage[y][x][0] = 128; // Gray for masked area
                    codeImage[y][x][1] = 128;
                    codeImage[y][x][2] = 128;
                } else {
                    int colorValue = template.code[y][x] ? 255 : 0;
                    codeImage[y][x][0] = colorValue;
                    codeImage[y][x][1] = colorValue;
                    codeImage[y][x][2] = colorValue;
                }
            }
        }
        return codeImage;
    }

    /**
     * Stitches the original image and the visual barcode together vertically.
     * Scales the barcode to match the width of the original image so it looks clean.
     */
    private int[][][] createCompositeMatrix(int[][][] original, int[][][] barcode) {
        int origH = original.length;
        int origW = original[0].length;

        int barH = barcode.length;    // 8
        int barW = barcode[0].length; // 256

        int targetBarH = 8;
        int targetBarW = origW;
        int padding = 0;

        int totalH = origH + padding + targetBarH;
        int[][][] composite = new int[totalH][origW][3];

        for (int y = 0; y < totalH; y++) {
            for (int x = 0; x < origW; x++) {
                composite[y][x][0] = 240;
                composite[y][x][1] = 240;
                composite[y][x][2] = 240;
            }
        }

        for (int y = 0; y < origH; y++) {
            for (int x = 0; x < origW; x++) {
                composite[y][x][0] = original[y][x][0];
                composite[y][x][1] = original[y][x][1];
                composite[y][x][2] = original[y][x][2];
            }
        }

        for (int y = 0; y < targetBarH; y++) {
            for (int x = 0; x < targetBarW; x++) {
                int srcY = (int) (y * ((double) barH / targetBarH));
                int srcX = (int) (x * ((double) barW / targetBarW));

                composite[origH + padding + y][x][0] = barcode[srcY][srcX][0];
                composite[origH + padding + y][x][1] = barcode[srcY][srcX][1];
                composite[origH + padding + y][x][2] = barcode[srcY][srcX][2];
            }
        }

        return composite;
    }
}