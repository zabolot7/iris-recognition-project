package optionspanels;

import core.ImageProcessor;
import core.IrisRecognitionProcessor;
import core.MenuBar;
import core.OptionPanel;
import core.PhotoPanel;

import javax.imageio.ImageIO;
import javax.swing.*;
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

    public IrisRecognitionPanel(PhotoPanel photoPanel, OptionPanel parentPanel) {
        this.photoPanel = photoPanel;
        this.parentPanel = parentPanel;

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(30, 20, 0, 20));

        this.originalMatrix = photoPanel.getImageMatrix();
        parentPanel.saveUndoState(originalMatrix);
        boundaryMode = parentPanel.getBoundaryMode();

        loadDefaultImage();

        buildUI();
    }

    private void loadDefaultImage() {
        try {
            BufferedImage img = ImageIO.read(new File("src/MMU-Iris-Database/1/left/aeval1.bmp"));
            originalMatrix = photoPanel.createImageMatrix(img);
            photoPanel.setImageMatrix(originalMatrix);

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

        JButton generateCodeBtn = new JButton("9. Generate iris code (mask)");
        generateCodeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        generateCodeBtn.setEnabled(false);

        choosePictureBtn.addActionListener(e -> {
            File file = MenuBar.chooseImageFile(this, "Select First Iris Image");
            if (file == null) return;

            try {
                BufferedImage img1 = ImageIO.read(file);

                originalMatrix = photoPanel.createImageMatrix(img1);

                photoPanel.setImageMatrix(originalMatrix);
                pupilBinarizationBtn.setEnabled(false);
                pupilMorphologyBtn.setEnabled(false);
                pupilBoundariesBtn.setEnabled(false);
                revertGrayscaleBtn.setEnabled(false);
                allBoundariesBtn.setEnabled(false);
                irisBoundariesBtn.setEnabled(false);
                generateCodeBtn.setEnabled(false);
                getIrisRectangleBtn.setEnabled(false);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error reading image file.", "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        grayscaleBtn.addActionListener(e -> {
            parentPanel.saveUndoState(photoPanel.getImageMatrix());
            int[][][] newMatrix = ImageProcessor.applyGrayscale(photoPanel.getImageMatrix(), GrayscalePanel.GrayscaleOptions.LUMINANCE);
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
            parentPanel.saveUndoState(photoPanel.getImageMatrix());
            if (grayscaledMatrix != null) {
                photoPanel.setImageMatrix(grayscaledMatrix);
            } else {
                photoPanel.setImageMatrix(originalMatrix);
            }
            parentPanel.updateProjections();
        });

        pupilBinarizationBtn.addActionListener(e -> {
            if (!isGrayscaleApplied) return;

            parentPanel.saveUndoState(photoPanel.getImageMatrix());
            int[][][] newMatrix = IrisRecognitionProcessor.applyPupilBinarization(photoPanel.getImageMatrix());
            photoPanel.setImageMatrix(newMatrix);
            parentPanel.updateProjections();

            isBinarized = true;
        });

        pupilMorphologyBtn.addActionListener(e -> {
            if (!isBinarized) return;

            parentPanel.saveUndoState(photoPanel.getImageMatrix());
            int[][][] newMatrix = IrisRecognitionProcessor.applyPupilMorphology(photoPanel.getImageMatrix(), boundaryMode);
            photoPanel.setImageMatrix(newMatrix);
            parentPanel.updateProjections();
        });

        pupilBoundariesBtn.addActionListener(e -> {
            if (!isBinarized) return;

            parentPanel.saveUndoState(photoPanel.getImageMatrix());
            eyeCenter = IrisRecognitionProcessor.calculateCenter(photoPanel.getImageMatrix());
            pupilRadius = IrisRecognitionProcessor.calculateRadius(photoPanel.getImageMatrix(), eyeCenter);
            int[][][] newMatrix = IrisRecognitionProcessor.applyBoundaries(photoPanel.getImageMatrix(), eyeCenter, pupilRadius);
            photoPanel.setImageMatrix(newMatrix);

            irisBoundariesBtn.setEnabled(true);
        });

        irisBoundariesBtn.addActionListener(e -> {
            if (!isBinarized) return;

            parentPanel.saveUndoState(photoPanel.getImageMatrix());
            irisRadius = IrisRecognitionProcessor.calculateDaugmanIrisRadius(grayscaledMatrix, eyeCenter, pupilRadius);
            int[][][] newMatrix = IrisRecognitionProcessor.applyBoundaries(photoPanel.getImageMatrix(), eyeCenter, irisRadius);
            photoPanel.setImageMatrix(newMatrix);

            allBoundariesBtn.setEnabled(true);
        });

        allBoundariesBtn.addActionListener(e -> {
            parentPanel.saveUndoState(photoPanel.getImageMatrix());

            int[][][] newMatrix = IrisRecognitionProcessor.applyBoundaries(originalMatrix, eyeCenter, pupilRadius);
            newMatrix = IrisRecognitionProcessor.applyBoundaries(newMatrix, eyeCenter, irisRadius);

            photoPanel.setImageMatrix(newMatrix);
            parentPanel.updateProjections();

            getIrisRectangleBtn.setEnabled(true);
        });

        getIrisRectangleBtn.addActionListener(e -> {
            parentPanel.saveUndoState(photoPanel.getImageMatrix());
            int[][][] newMatrix = IrisRecognitionProcessor.generateIrisRectangle(originalMatrix, eyeCenter, pupilRadius, irisRadius);
            photoPanel.setImageMatrix(newMatrix);
            parentPanel.updateProjections();

            generateCodeBtn.setEnabled(true);
        });

        generateCodeBtn.addActionListener(e -> {
            parentPanel.saveUndoState(photoPanel.getImageMatrix());

            int[][][] unwrappedIris = photoPanel.getImageMatrix();

            IrisRecognitionProcessor.IrisTemplate template = IrisRecognitionProcessor.extractIrisCode(unwrappedIris);

            int[][][] codeImage = IrisRecognitionProcessor.createVisualBarcode(template);

            int[][][] fullImage = IrisRecognitionProcessor.createCompositeMatrix(unwrappedIris, codeImage);

            photoPanel.setImageMatrix(fullImage);
            parentPanel.updateProjections();
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
        this.add(Box.createVerticalGlue());
        this.add(Box.createVerticalStrut(20));
        this.add(generateCodeBtn);
    }
}
