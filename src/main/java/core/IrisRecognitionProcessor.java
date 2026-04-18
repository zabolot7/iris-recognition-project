package core;

public class IrisRecognitionProcessor {

    public static int[][][] applyEyeBinarization(int[][][] originalMatrix, double divider) {
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
        P = (int) Math.round(P / (height * width * divider));

        return ImageProcessor.applySegmentation(originalMatrix, P);
    }

    public static int[][][] applyPupilBinarization(int[][][] originalMatrix) {
        return applyEyeBinarization(originalMatrix, 15);
    }

    public static int[][][] applyIrisBinarization(int[][][] originalMatrix) {
        return applyEyeBinarization(originalMatrix, 4.5);
    }

    private static boolean[][] createDisk(int radius) {
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
        if (originalMatrix == null) return null;

        boolean[][] disk11 = createDisk(5);
        boolean[][] disk15 = createDisk(7);

        int[][][] temp = ImageProcessor.applyOpening(originalMatrix, disk11, mode);
        return ImageProcessor.applyClosing(temp, disk15, mode);
    }

    public static int[][][] applyIrisMorphology(int[][][] originalMatrix, OptionPanel.BoundaryMode mode) {
        if (originalMatrix == null) return null;

        boolean[][] disk7 = createDisk(3);
        boolean[][] disk25 = createDisk(12);

        int[][][] temp = ImageProcessor.applyOpening(originalMatrix, disk7, mode);
        return ImageProcessor.applyClosing(temp, disk25, mode);
    }

    public static int[] calculateCenter(int[][][] originalMatrix) {
        if (originalMatrix == null) return null;

        int height = originalMatrix.length;
        int width = originalMatrix[0].length;

        // we're skipping the margins to avoid background noise
        int marginX = width / 4;
        int marginY = height / 4;

        int[] verticalProj = new int[width];
        int[] horizontalProj = new int[height];

        for (int y = marginY; y < height - marginY; y++) {
            for (int x = marginX; x < width - marginX; x++) {
                if (originalMatrix[y][x][0] == 0) {
                    verticalProj[x]++;
                    horizontalProj[y]++;
                }
            }
        }

        int max = -1;
        int firstMaxI = 0;
        int lastMaxI = 0;

        for (int i = 0; i < verticalProj.length; i++) {
            if (verticalProj[i] > max) {
                max = verticalProj[i];
                firstMaxI = i;
                lastMaxI = i;
            } else if (verticalProj[i] == max) {
                lastMaxI = i;
            }
        }
        int centerX = (firstMaxI + lastMaxI) / 2;

        max = -1;
        firstMaxI = 0;
        lastMaxI = 0;

        for (int i = 0; i < horizontalProj.length; i++) {
            if (horizontalProj[i] > max) {
                max = horizontalProj[i];
                firstMaxI = i;
                lastMaxI = i;
            } else if (horizontalProj[i] == max) {
                lastMaxI = i;
            }
        }
        int centerY = (firstMaxI + lastMaxI) / 2;

        return new int[] {centerX, centerY};
    }

    public static int calculateRadius(int[][][] originalMatrix, int[] center) {
        if (originalMatrix == null) return 0;

        int[][] projections = ImageProcessor.getProjections(originalMatrix);
        int[] verticalProj = projections[0];
        int[] horizontalProj = projections[1];

        return verticalProj[center[0]]/2;
    }

    public static int calculateDaugmanIrisRadius(int[][][] grayscaleMatrix, int[] center, int pupilRadius) {
        if (grayscaleMatrix == null || center == null) return 0;

        int cX = center[0];
        int cY = center[1];
        int width = grayscaleMatrix[0].length;
        int height = grayscaleMatrix.length;

        int maxRadius = Math.min(width / 2, height / 2);

        double[] averageIntensities = new double[maxRadius];

        for (int r = pupilRadius + 10; r < maxRadius; r++) {
            double sumIntensity = 0;
            int pixelCount = 0;

            int numPoints = (int) (2 * Math.PI * r);

            for (int i = 0; i < numPoints; i++) {
                double angle = (2 * Math.PI * i) / numPoints;

               double deg = Math.toDegrees(angle);
                if ((deg > 45 && deg < 135) || (deg > 225 && deg < 315)) {
                    continue;
                }

                int x = (int) Math.round(cX + r * Math.cos(angle));
                int y = (int) Math.round(cY + r * Math.sin(angle));

                if (x >= 0 && x < width && y >= 0 && y < height) {
                    sumIntensity += grayscaleMatrix[y][x][0];
                    pixelCount++;
                }
            }

            if (pixelCount > 0) {
                averageIntensities[r] = sumIntensity / pixelCount;
            }
        }

        double maxDerivative = -1;
        int bestRadius = pupilRadius;

        int step = 3;

        for (int r = pupilRadius + 15; r < maxRadius - step; r++) {
            double derivative = averageIntensities[r + step] - averageIntensities[r - step];

            if (derivative > maxDerivative) {
                maxDerivative = derivative;
                bestRadius = r;
            }
        }

        return bestRadius;
    }


    public static int[][][] applyBoundaries(int[][][] originalMatrix, int[] center, int radius) {
        if (originalMatrix == null || center == null) return null;

        int height = originalMatrix.length;
        int width = originalMatrix[0].length;

        int[][][] newMatrix = new int[height][width][3];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                newMatrix[y][x][0] = originalMatrix[y][x][0];
                newMatrix[y][x][1] = originalMatrix[y][x][1];
                newMatrix[y][x][2] = originalMatrix[y][x][2];
            }
        }

        int cX = center[0];
        int cY = center[1];

        newMatrix[cY][cX][0] = 255;
        newMatrix[cY][cX][1] = 0;
        newMatrix[cY][cX][2] = 0;

        int x = radius;
        int y = 0;
        int err = 0;

        while (x >= y) {
            int[][] points = {
                    {cX + x, cY + y}, {cX + y, cY + x}, {cX - y, cY + x}, {cX - x, cY + y},
                    {cX - x, cY - y}, {cX - y, cY - x}, {cX + y, cY - x}, {cX + x, cY - y}
            };

            for (int[] p : points) {
                int px = p[0];
                int py = p[1];
                if (px >= 0 && px < width && py >= 0 && py < height) {
                    newMatrix[py][px][0] = 255;
                    newMatrix[py][px][1] = 0;
                    newMatrix[py][px][2] = 0;
                }
            }

            if (err <= 0) {
                y += 1;
                err += 2 * y + 1;
            }
            if (err > 0) {
                x -= 1;
                err -= 2 * x + 1;
            }
        }

        return newMatrix;
    }

    public static int[][][] generateIrisRectangle(int[][][] originalMatrix, int[] center, int pupilRadius, int irisRadius) {
        if (originalMatrix == null || center == null) return null;

        int height = originalMatrix.length;
        int width = originalMatrix[0].length;
        int[][][] newMatrix = new int[64][512][3];

        int cX = center[0];
        int cY = center[1];

        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 512; x++) {
                double theta = 2 * Math.PI * x / 512.0;
                double radius = pupilRadius + (y / 64.0) * (irisRadius - pupilRadius);

                int xOriginal = (int) Math.round(cX + radius * Math.cos(theta));
                int yOriginal = (int) Math.round(cY + radius * Math.sin(theta));

                if (xOriginal >= 0 && xOriginal < width && yOriginal >= 0 && yOriginal < height) {
                    newMatrix[y][x][0] = originalMatrix[yOriginal][xOriginal][0];
                    newMatrix[y][x][1] = originalMatrix[yOriginal][xOriginal][1];
                    newMatrix[y][x][2] = originalMatrix[yOriginal][xOriginal][2];
                } else {
                    newMatrix[y][x][0] = 0;
                    newMatrix[y][x][1] = 0;
                    newMatrix[y][x][2] = 0;
                }
            }
        }

        return newMatrix;
    }

}
