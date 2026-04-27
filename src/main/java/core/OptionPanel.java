package core;

import javax.swing.*;
import java.awt.*;

/**
 * The sidebar panel for additional user input for various image editing features.
 * At the top, it displays a Tool Area for the particular image editing feature.
 */
public class OptionPanel extends JPanel {

    /**
     * Defines how convolution filters handle pixels outside the image.
     */
    public enum BoundaryMode {
        CROP, KEEP_ORIGINAL, PAD_BLACK, PAD_WHITE, PAD_GRAY, REPLICATE, MIRROR
    }

    private PhotoPanel photoPanel;
    private BoundaryMode currentBoundaryMode = BoundaryMode.REPLICATE;
    private JPanel toolArea;

    /**
     * Constructs the sidebar panel.
     *
     * @param photoPanel The image panel controlled / referenced by this option panel.
     */
    public OptionPanel(PhotoPanel photoPanel) {
        this.photoPanel = photoPanel;

        this.setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(400, 0));

        toolArea = new JPanel(new BorderLayout());
        this.add(toolArea, BorderLayout.NORTH);
    }

    /**
     * Retrieves the current boundary handling mode.
     *
     * @return The active BoundaryMode enum.
     */
    public BoundaryMode getBoundaryMode() {
        return currentBoundaryMode;
    }

    /**
     * Swaps out the currently active tool interface for a new one.
     *
     * @param toolPanel The JPanel containing the UI for the newly selected tool.
     */
    public void loadToolPanel(JPanel toolPanel) {
        toolArea.removeAll();

        toolArea.add(toolPanel, BorderLayout.CENTER);

        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.revalidate();
        }

        toolArea.revalidate();
        toolArea.repaint();
    }

    /**
     * Updates the projections.
     */
    public void updateProjections() {
        photoPanel.updateProjections();
    }

}
