package optionspanels;

import core.ImageProcessor;
import core.IrisRecognitionProcessor;
import core.MenuBar;
import core.OptionPanel;
import core.PhotoPanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class IrisRecognitionPanel extends JPanel{
    private PhotoPanel photoPanel;
    private OptionPanel parentPanel;
    private int[][][] originalMatrix;
    private boolean isGrayscaleApplied = false;
    private boolean isBinarized = false;
    private int[][][] grayscaledMatrix;
    private OptionPanel.BoundaryMode boundaryMode;
    private int[] eyeCenter;
    private int irisRadius;
    private int pupilRadius;
    private int[][][] barcodeMatrix;

    public IrisRecognitionPanel(PhotoPanel photoPanel, OptionPanel parentPanel) {
        this.photoPanel = photoPanel;
        this.parentPanel = parentPanel;

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(30, 20, 0, 20));

        this.originalMatrix = photoPanel.getImageMatrix();
        boundaryMode = parentPanel.getBoundaryMode();

        loadDefaultImage();

        buildUI();
    }

    private void loadDefaultImage() {
        try {
            BufferedImage img = ImageIO.read(new File("src/MMU-Iris-Database/1/left/aeval1.bmp"));
            originalMatrix = photoPanel.createImageMatrix(img);
            photoPanel.setImageMatrix(originalMatrix);
            photoPanel.setCurrentFilename("aeval1");

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not load default image.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buildUI() {
        JLabel titleLabel = new JLabel("Prepare for iris recognition:");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JButton choosePictureBtn = new JButton("0. Choose a picture");
        choosePictureBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton grayscaleBtn = new JButton("1. Convert to Grayscale");
        grayscaleBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton pupilBinarizationBtn = new JButton("2. Apply binarization for the pupil");
        pupilBinarizationBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        pupilBinarizationBtn.setEnabled(false);

        JButton pupilMorphologyBtn = new JButton("3. Apply morphology operations");
        pupilMorphologyBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        pupilMorphologyBtn.setEnabled(false);

        JButton pupilBoundariesBtn = new JButton("4. Find the pupil boundaries");
        pupilBoundariesBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        pupilBoundariesBtn.setEnabled(false);

        JButton revertGrayscaleBtn = new JButton("5. Revert to grayscaled image");
        revertGrayscaleBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        revertGrayscaleBtn.setEnabled(false);

        JButton irisBoundariesBtn = new JButton("6. Find the iris boundaries");
        irisBoundariesBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        irisBoundariesBtn.setEnabled(false);

        JButton allBoundariesBtn = new JButton("7. Visualize both boundaries");
        allBoundariesBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        allBoundariesBtn.setEnabled(false);

        JButton getIrisRectangleBtn = new JButton("8. Unwrap the iris to rectangular block");
        getIrisRectangleBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        getIrisRectangleBtn.setEnabled(false);

        JButton generateCodeBtn = new JButton("9. Generate iris code (with mask)");
        generateCodeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        generateCodeBtn.setEnabled(false);

        JButton saveCodeBtn = new JButton("10. Save iris code");
        saveCodeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveCodeBtn.setEnabled(false);

        choosePictureBtn.addActionListener(e -> {
            File file = MenuBar.chooseImageFile(this, "Select First Iris Image");
            if (file == null) return;

            try {
                BufferedImage img1 = ImageIO.read(file);

                originalMatrix = photoPanel.createImageMatrix(img1);

                photoPanel.setImageMatrix(originalMatrix);
                photoPanel.setCurrentFilename(file.getName());
                pupilBinarizationBtn.setEnabled(false);
                pupilMorphologyBtn.setEnabled(false);
                pupilBoundariesBtn.setEnabled(false);
                revertGrayscaleBtn.setEnabled(false);
                allBoundariesBtn.setEnabled(false);
                irisBoundariesBtn.setEnabled(false);
                generateCodeBtn.setEnabled(false);
                getIrisRectangleBtn.setEnabled(false);
                saveCodeBtn.setEnabled(false);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error reading image file.", "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        grayscaleBtn.addActionListener(e -> {
            int[][][] newMatrix = ImageProcessor.applyGrayscale(photoPanel.getImageMatrix(), ImageProcessor.GrayscaleOptions.LUMINANCE);
            photoPanel.setImageMatrix(newMatrix);
            isGrayscaleApplied = true;
            parentPanel.updateProjections();

            grayscaledMatrix = newMatrix;
            pupilBinarizationBtn.setEnabled(true);
            pupilMorphologyBtn.setEnabled(true);
            pupilBoundariesBtn.setEnabled(true);
            revertGrayscaleBtn.setEnabled(true);

        });

        revertGrayscaleBtn.addActionListener(e -> {
            if (grayscaledMatrix != null) {
                photoPanel.setImageMatrix(grayscaledMatrix);
            } else {
                photoPanel.setImageMatrix(originalMatrix);
            }
            parentPanel.updateProjections();
        });

        pupilBinarizationBtn.addActionListener(e -> {
            if (!isGrayscaleApplied) return;

            int[][][] newMatrix = IrisRecognitionProcessor.applyPupilBinarization(photoPanel.getImageMatrix());
            photoPanel.setImageMatrix(newMatrix);
            parentPanel.updateProjections();

            isBinarized = true;
        });

        pupilMorphologyBtn.addActionListener(e -> {
            if (!isBinarized) return;

            int[][][] newMatrix = IrisRecognitionProcessor.applyPupilMorphology(photoPanel.getImageMatrix(), boundaryMode);
            photoPanel.setImageMatrix(newMatrix);
            parentPanel.updateProjections();
        });

        pupilBoundariesBtn.addActionListener(e -> {
            if (!isBinarized) return;

            eyeCenter = IrisRecognitionProcessor.calculateCenter(photoPanel.getImageMatrix());
            pupilRadius = IrisRecognitionProcessor.calculateRadius(photoPanel.getImageMatrix(), eyeCenter);
            int[][][] newMatrix = IrisRecognitionProcessor.applyBoundaries(photoPanel.getImageMatrix(), eyeCenter, pupilRadius);
            photoPanel.setImageMatrix(newMatrix);

            irisBoundariesBtn.setEnabled(true);
        });

        irisBoundariesBtn.addActionListener(e -> {
            if (!isBinarized) return;

            irisRadius = IrisRecognitionProcessor.calculateDaugmanIrisRadius(grayscaledMatrix, eyeCenter, pupilRadius);
            int[][][] newMatrix = IrisRecognitionProcessor.applyBoundaries(photoPanel.getImageMatrix(), eyeCenter, irisRadius);
            photoPanel.setImageMatrix(newMatrix);

            allBoundariesBtn.setEnabled(true);
        });

        allBoundariesBtn.addActionListener(e -> {

            int[][][] newMatrix = IrisRecognitionProcessor.applyBoundaries(originalMatrix, eyeCenter, pupilRadius);
            newMatrix = IrisRecognitionProcessor.applyBoundaries(newMatrix, eyeCenter, irisRadius);

            photoPanel.setImageMatrix(newMatrix);
            parentPanel.updateProjections();

            getIrisRectangleBtn.setEnabled(true);
        });

        getIrisRectangleBtn.addActionListener(e -> {
            int[][][] newMatrix = IrisRecognitionProcessor.generateIrisRectangle(originalMatrix, eyeCenter, pupilRadius, irisRadius);
            photoPanel.setImageMatrix(newMatrix);
            parentPanel.updateProjections();

            generateCodeBtn.setEnabled(true);
        });

        generateCodeBtn.addActionListener(e -> {

            int[][][] unwrappedIris = photoPanel.getImageMatrix();

            IrisRecognitionProcessor.IrisTemplate template = IrisRecognitionProcessor.extractIrisCode(unwrappedIris);

            int[][][] codeImage = IrisRecognitionProcessor.createVisualBarcode(template);
            barcodeMatrix = codeImage;

            int[][][] fullImage = IrisRecognitionProcessor.createCompositeMatrix(unwrappedIris, codeImage);

            photoPanel.setImageMatrix(fullImage);
            parentPanel.updateProjections();
            saveCodeBtn.setEnabled(true);
        });

        saveCodeBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Save Iris Code");

            // filetype filters
            FileNameExtensionFilter pngFilter = new FileNameExtensionFilter("PNG Image (*.png)", "png");
            FileNameExtensionFilter jpgFilter = new FileNameExtensionFilter("JPEG Image (*.jpg; *.jpeg)", "jpg", "jpeg");
            FileNameExtensionFilter bmpFilter = new FileNameExtensionFilter("BMP Image (*.bmp)", "bmp");

            fc.addChoosableFileFilter(pngFilter);
            fc.addChoosableFileFilter(jpgFilter);
            fc.addChoosableFileFilter(bmpFilter);

            // png as default
            fc.setFileFilter(pngFilter);
            fc.setAcceptAllFileFilterUsed(false);

            // dynamically get the original filename from the JFrame's title
            String originalName = photoPanel.getCurrentFilename();
            String baseName = "iris_code";

            if (originalName != null && !originalName.equals("untitled_image")) {
                int dotIndex = originalName.lastIndexOf('.');
                if (dotIndex > 0) {
                    baseName = originalName.substring(0, dotIndex) + "_code";
                } else {
                    baseName = originalName + "_code";
                }
            }

            // Set the default name (e.g., "aeval1_code")
            fc.setSelectedFile(new File(baseName));

            int result = fc.showSaveDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fc.getSelectedFile();
                String filePath = fileToSave.getAbsolutePath();

                javax.swing.filechooser.FileFilter selectedFilter = fc.getFileFilter();
                String format = "png";
                if (selectedFilter == jpgFilter) {
                    format = "jpg";
                } else if (selectedFilter == bmpFilter) {
                    format = "bmp";
                }

                // add file extension if missing
                if (!filePath.toLowerCase().endsWith("." + format)) {
                    fileToSave = new File(filePath + "." + format);
                }

                try {
                    java.awt.image.BufferedImage imageToSave = null;

                    if (barcodeMatrix != null) {
                        int h = barcodeMatrix.length;
                        int w = barcodeMatrix[0].length;
                        imageToSave = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);

                        for (int y = 0; y < h; y++) {
                            for (int x = 0; x < w; x++) {
                                int r = barcodeMatrix[y][x][0];
                                int g = barcodeMatrix[y][x][1];
                                int b = barcodeMatrix[y][x][2];
                                int rgb = (255 << 24) | (r << 16) | (g << 8) | b;
                                imageToSave.setRGB(x, y, rgb);
                            }
                        }
                    }

                    if (imageToSave != null) {
                        ImageIO.write(imageToSave, format, fileToSave);
                        JOptionPane.showMessageDialog(this,
                                "Iris code saved successfully!",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "There is no iris code to save.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this,
                            "Error saving iris code: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        });

        this.add(titleLabel);
        this.add(Box.createVerticalStrut(20));
        this.add(choosePictureBtn);
        this.add(Box.createVerticalStrut(20));
        this.add(grayscaleBtn);
        this.add(Box.createVerticalStrut(20));
        this.add(pupilBinarizationBtn);
        this.add(Box.createVerticalStrut(20));
        this.add(pupilMorphologyBtn);
        this.add(Box.createVerticalStrut(20));
        this.add(pupilBoundariesBtn);
        this.add(Box.createVerticalStrut(20));
        this.add(revertGrayscaleBtn);
        this.add(Box.createVerticalStrut(20));
        this.add(irisBoundariesBtn);
        this.add(Box.createVerticalStrut(20));
        this.add(allBoundariesBtn);
        this.add(Box.createVerticalStrut(20));
        this.add(getIrisRectangleBtn);
        this.add(Box.createVerticalStrut(20));
        this.add(generateCodeBtn);
        this.add(Box.createVerticalStrut(20));
        this.add(saveCodeBtn);
        this.add(Box.createVerticalGlue());
    }
}
