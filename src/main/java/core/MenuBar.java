package core;

import optionspanels.IrisRecognitionPanel;
import optionspanels.IrisComparisonPanel;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * Constructs and manages the main application menu bar.
 * Contains tabs to switch between Iris Recognition and Iris Comparison modes.
 */
public class MenuBar extends JMenuBar {
    private PhotoPanel photoPanel;
    private OptionPanel optionPanel;
    private JFrame frame;

    /**
     * Initializes the menu bar with direct, clickable tabs.
     *
     * @param frame The parent JFrame.
     * @param photoPanel The main image display panel.
     * @param optionPanel The sidebar tool panel.
     */
    public MenuBar(JFrame frame, PhotoPanel photoPanel, OptionPanel optionPanel) {
        this.frame = frame;
        this.photoPanel = photoPanel;
        this.optionPanel = optionPanel;

        // 1. Create the top-level menus (acting as tabs)
        JMenu recognitionTab = new JMenu("Iris Recognition");
        JMenu comparisonTab = new JMenu("Iris Comparison");

        // 2. Add mouse listeners so they click like buttons
        recognitionTab.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                optionPanel.loadToolPanel(new IrisRecognitionPanel(photoPanel, optionPanel));
            }
        });

        comparisonTab.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                optionPanel.loadToolPanel(new IrisComparisonPanel(photoPanel, optionPanel));
            }
        });

        // 3. Add them to the MenuBar
        add(recognitionTab);
        add(comparisonTab);
    }

    /**
     * A reusable helper method to open a JFileChooser.
     * Kept here because IrisRecognitionPanel and IrisComparisonPanel rely on it!
     * * @param parentComponent The UI component calling this dialog (for centering).
     * @param dialogTitle The title of the window.
     * @return The selected File object, or null if the user canceled.
     */
    public static File chooseImageFile(java.awt.Component parentComponent, String dialogTitle) {
        JFileChooser fc = new JFileChooser();

        if (dialogTitle != null && !dialogTitle.isEmpty()) {
            fc.setDialogTitle(dialogTitle);
        }

        FileNameExtensionFilter filter = new FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "bmp");
        fc.setFileFilter(filter);
        fc.setAcceptAllFileFilterUsed(false);

        fc.setCurrentDirectory(new File(System.getProperty("user.dir")));

        int result = fc.showOpenDialog(parentComponent);

        if (result == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile();
        }

        return null;
    }
}