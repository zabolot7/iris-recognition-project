package core;

public class IrisRecognitionProcessor {

    public static int[][][] applyEyeBinarization(int[][][] originalMatrix, int divider) {
        if (originalMatrix == null) return null;

        int height = originalMatrix.length;
        int width = originalMatrix[0].length;
        int[][][] newMatrix = new int[height][width][3];
        int P = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int c = 0; c < 3; c++) {
                    P += originalMatrix[y][x][c];
                }
            }
        }
        P = P / (height * width * divider);

        return ImageProcessor.applySegmentation(originalMatrix, P);
    }

    public static int[][][] applyPupilBinarization(int[][][] originalMatrix) {
        return applyEyeBinarization(originalMatrix, 15);
    }

    public static int[][][] applyIrisBinarization(int[][][] originalMatrix) {
        return applyEyeBinarization(originalMatrix, 5);
    }

    public static boolean[][] createDisk(int radius) {
        int diameter = 2 * radius + 1;
        boolean[][] disk = new boolean[diameter][diameter];
        int center = radius;

        for (int y = 0; y < diameter; y++) {
            for (int x = 0; x < diameter; x++) {
                int dx = x - center;
                int dy = y - center;

                if (dx * dx + dy * dy <= radius * radius) {
                    disk[y][x] = true;
                }
            }
        }
        return disk;
    }

    public static int[][][] applyPupilMorphology(int[][][] originalMatrix, OptionPanel.BoundaryMode mode) {
        boolean[][] disk11 = createDisk(5);
        boolean[][] disk15 = createDisk(7);

        int[][][] temp = ImageProcessor.applyOpening(originalMatrix, disk11, mode);
        return ImageProcessor.applyClosing(temp, disk15, mode);
    }

    public static int[][][] applyIrisMorphology(int[][][] originalMatrix, OptionPanel.BoundaryMode mode) {
        boolean[][] disk7 = createDisk(3);
        boolean[][] disk25 = createDisk(12);

        int[][][] temp = ImageProcessor.applyOpening(originalMatrix, disk7, mode);
        return ImageProcessor.applyClosing(temp, disk25, mode);
    }

}
