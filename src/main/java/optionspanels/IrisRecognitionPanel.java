package optionspanels;

import core.ImageProcessor;
import core.IrisRecognitionProcessor;
import core.OptionPanel;
import core.PhotoPanel;

import javax.swing.*;
import java.awt.*;

public class IrisRecognitionPanel extends JPanel{
    private PhotoPanel photoPanel;
    private OptionPanel parentPanel;
    private int[][][] originalMatrix;
    private boolean isGrayscaleApplied = false;
    private boolean isBinarized = false;
    private int[][][] grayscaledMatrix;
    private OptionPanel.BoundaryMode boundaryMode;

    public IrisRecognitionPanel(PhotoPanel photoPanel, OptionPanel parentPanel) {
        this.photoPanel = photoPanel;
        this.parentPanel = parentPanel;

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(30, 20, 0, 20));

        this.originalMatrix = photoPanel.getImageMatrix();
        parentPanel.saveUndoState(originalMatrix);

        boundaryMode = parentPanel.getBoundaryMode();

        buildUI();
    }

    private void buildUI() {
        JLabel titleLabel = new JLabel("Prepare for iris recognition:");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JButton grayscaleBtn = new JButton("1. Convert to Grayscale");
        grayscaleBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton pupilBinarizationBtn = new JButton("2. Apply binarization for the pupil");
        pupilBinarizationBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        //pupilBinarizationBtn.setEnabled(false);

        JButton pupilMorphologyBtn = new JButton("3. Apply morphology operations");
        pupilMorphologyBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        pupilMorphologyBtn.addActionListener(e -> applyMorphology(true));

        JButton revertGrayscaleBtn = new JButton("4. Revert to grayscaled image");
        revertGrayscaleBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        //revertGrayscaleBtn.setEnabled(false);

        JButton irisBinarizationBtn = new JButton("5. Apply binarization for the iris");
        irisBinarizationBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        //irisBinarizationBtn.setEnabled(false);

        JButton irisMorphologyBtn = new JButton("6. Apply morphology operations");
        irisMorphologyBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        irisMorphologyBtn.addActionListener(e -> applyMorphology(false));

        grayscaleBtn.addActionListener(e -> {
            parentPanel.saveUndoState(photoPanel.getImageMatrix());
            int[][][] newMatrix = ImageProcessor.applyGrayscale(photoPanel.getImageMatrix(), GrayscalePanel.GrayscaleOptions.LUMINANCE);
            photoPanel.setImageMatrix(newMatrix);
            originalMatrix = newMatrix;
            isGrayscaleApplied = true;

            grayscaledMatrix = newMatrix;
        });

        revertGrayscaleBtn.addActionListener(e -> {
            parentPanel.saveUndoState(photoPanel.getImageMatrix());
            if (grayscaledMatrix != null) {
                photoPanel.setImageMatrix(grayscaledMatrix);
            } else {
                photoPanel.setImageMatrix(originalMatrix);
            }
        });

        pupilBinarizationBtn.addActionListener(e -> {
            if (!isGrayscaleApplied) return;

            parentPanel.saveUndoState(photoPanel.getImageMatrix());
            int[][][] newMatrix = IrisRecognitionProcessor.applyPupilBinarization(photoPanel.getImageMatrix());
            photoPanel.setImageMatrix(newMatrix);
            originalMatrix = newMatrix;

            isBinarized = true;
        });

        irisBinarizationBtn.addActionListener(e -> {
            if (!isGrayscaleApplied) return;

            parentPanel.saveUndoState(photoPanel.getImageMatrix());
            int[][][] newMatrix = IrisRecognitionProcessor.applyIrisBinarization(photoPanel.getImageMatrix());
            photoPanel.setImageMatrix(newMatrix);
            originalMatrix = newMatrix;

            isBinarized = true;
        });

        this.add(titleLabel);
        this.add(Box.createVerticalStrut(20));
        this.add(grayscaleBtn);
        this.add(Box.createVerticalStrut(20));
        this.add(pupilBinarizationBtn);
        this.add(Box.createVerticalStrut(20));
        this.add(pupilMorphologyBtn);
        this.add(Box.createVerticalStrut(20));
        this.add(revertGrayscaleBtn);
        this.add(Box.createVerticalStrut(20));
        this.add(irisBinarizationBtn);
        this.add(Box.createVerticalStrut(20));
        this.add(irisMorphologyBtn);
        this.add(Box.createVerticalGlue());
    }

    private void applyMorphology(boolean pupil) {
        if (!isBinarized) return;

        parentPanel.saveUndoState(photoPanel.getImageMatrix());
        int[][][] newMatrix = photoPanel.getImageMatrix();

        if (pupil) {
            newMatrix = IrisRecognitionProcessor.applyPupilMorphology(originalMatrix, boundaryMode);
        } else {
            newMatrix = IrisRecognitionProcessor.applyIrisMorphology(originalMatrix, boundaryMode);
        }

        photoPanel.setImageMatrix(newMatrix);
        originalMatrix = newMatrix;
    }
}
