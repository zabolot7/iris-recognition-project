package core;

import optionspanels.GrayscalePanel;

public class IrisRecognitionProcessor {

    /**
     * Dynamically calculates a binarization threshold based on the image's average global intensity
     * and applies segmentation.
     *
     * @param originalMatrix The 3D array representing the image.
     * @param divider        The divisor used to scale down the average intensity (higher values yield a stricter, darker threshold).
     * @return A 3D array representing the resulting binarized image.
     */
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

    /**
     * Applies binarization to isolate the pupil using a threshold divisor of 15.
     *
     * @param originalMatrix The 3D array representing the image.
     * @return The 3D array representing the image after binarization.
     */
    public static int[][][] applyPupilBinarization(int[][][] originalMatrix) {
        return applyEyeBinarization(originalMatrix, 15);
    }

    /**
     * A helper function to generate a circular structuring element (disk) for morphological operations.
     *
     * @param radius The radius of the disk in pixels.
     * @return A 2D boolean array representing the disk mask.
     */
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

    /**
     * Cleans and smooths the binarized pupil mask using sequential morphological operations.
     * Uses a 5-pixel radius disk as a structuring element for opening, followed by a 7-pixel radius disk a structuring element for closing.
     *
     * @param originalMatrix The 3D array representing the image.
     * @param mode           The boundary handling rule for edges.
     * @return The 3D array representing the image after morphology.
     */
    public static int[][][] applyPupilMorphology(int[][][] originalMatrix, OptionPanel.BoundaryMode mode) {
        if (originalMatrix == null) return null;

        boolean[][] disk11 = createDisk(5);
        boolean[][] disk15 = createDisk(7);

        int[][][] temp = ImageProcessor.applyOpening(originalMatrix, disk11, mode);
        return ImageProcessor.applyClosing(temp, disk15, mode);
    }

    /**
     * Calculates the eye center using the image's projections.
     * Outer margins are excluded from the analysis to prevent interference from peripheral noise like eyelashes or hair.
     *
     * @param originalMatrix The 3D array representing the image.
     * @return Calculated 2-element array containing the (x, y) coordinates of the pupil center.
     */
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

        return new int[]{centerX, centerY};
    }

    /**
     * Calculates the pupil radius using the image's vertical projection.
     *
     * @param originalMatrix The 3D array representing the image.
     * @param center         A 2-element array containing the (x, y) coordinates of the pupil center.
     * @return The calculated radius.
     */
    public static int calculateRadius(int[][][] originalMatrix, int[] center) {
        if (originalMatrix == null) return 0;

        int[][] projections = ImageProcessor.getProjections(originalMatrix);
        int[] verticalProj = projections[0];

        return verticalProj[center[0]] / 2;
    }

    /**
     * Calculates the iris radius using the Daugman's operator.
     *
     * @param grayscaleMatrix The 3D array representing the image in grayscale.
     * @param center          A 2-element array containing the (x, y) coordinates of the pupil center.
     * @param pupilRadius     The pupil radius.
     * @return The calculated iris radius.
     */
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

    /**
     * Draws the calculated boundary onto the given image.
     *
     * @param originalMatrix The 3D array representing the image.
     * @param center         A 2-element array containing the (x, y) coordinates of the pupil center.
     * @param radius         The boundary radius.
     * @return The 3D array representing the image with drawn boundaries.
     */
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

    /**
     * Generates an iris rectangle from the eye image using the given information.
     *
     * @param originalMatrix The 3D array representing the image.
     * @param center         A 2-element array containing the (x, y) coordinates of the pupil center.
     * @param pupilRadius    The pupil radius.
     * @param irisRadius     The iris radius.
     * @return A 3D array representing the iris rectangle.
     */
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

    /**
     * Full process of generating an iris rectangle from the eye image.
     *
     * @param originalMatrix The 3D array representing the image.
     * @param mode           The boundary handling rule for edges.
     * @return A 3D array representing the iris rectangle.
     */
    public static int[][][] extractIrisRectangle(int[][][] originalMatrix, OptionPanel.BoundaryMode mode) {
        if (originalMatrix == null) return null;

        int[][][] grayscaledMatrix = ImageProcessor.applyGrayscale(originalMatrix, GrayscalePanel.GrayscaleOptions.LUMINANCE);
        int[][][] newMatrix = applyPupilBinarization(grayscaledMatrix);
        newMatrix = applyPupilMorphology(newMatrix, mode);
        int[] eyeCenter = IrisRecognitionProcessor.calculateCenter(newMatrix);
        int pupilRadius = IrisRecognitionProcessor.calculateRadius(newMatrix, eyeCenter);
        int irisRadius = IrisRecognitionProcessor.calculateDaugmanIrisRadius(grayscaledMatrix, eyeCenter, pupilRadius);

        return generateIrisRectangle(grayscaledMatrix, eyeCenter, pupilRadius, irisRadius);
    }

    /**
     * Generates a boolean mask representing the valid sectors of the 8 rings as per angle limitations for each ring
     * defined in the Daugman algorithm.
     * True = legitimate iris area
     * False = excluded sector (often eyelids / eyelashes
     *
     * @return A 2D boolean array [64][512] representing the valid mask.
     */
    private static boolean[][] generateSectorMask() {
        boolean[][] mask = new boolean[64][512];

        for (int y = 0; y < 64; y++) {
            // determine which of the 8 bands the current pixel is in (0-7)
            int bandIndex = y / 8;

            for (int x = 0; x < 512; x++) {
                // convert x coordinate back to degrees (0-360)
                double deg = (x / 512.0) * 360.0;
                boolean isValid = true;

                // bands 1-4: exclude 30deg sector below pupil
                if (bandIndex <= 3) {
                    if (deg > 75.0 && deg < 105.0) {
                        isValid = false;
                    }
                }
                // bands 5-6: exclude 67deg top and bottom
                else if (bandIndex == 4 || bandIndex == 5) {
                    if ((deg > 236.5 && deg < 303.5) || (deg > 56.5 && deg < 123.5)) {
                        isValid = false;
                    }
                }
                // bands 7-8 : exclude 90deg top and bottom
                else {
                    if ((deg > 225.0 && deg < 315.0) || (deg > 45.0 && deg < 135.0)) {
                        isValid = false;
                    }
                }

                mask[y][x] = isValid;
            }
        }

        return mask;
    }

    public static class IrisTemplate {
        public boolean[][] code;
        public boolean[][] mask;

        public IrisTemplate(int rows, int cols) {
            code = new boolean[rows][cols];
            mask = new boolean[rows][cols];
        }
    }

    /**
     * Extracts the Iris Code using Daugman Algorithm.
     *
     * @param unwrappedIris The 64x512 unwrapped rectangular iris image.
     * @return An IrisTemplate object containing the 2048-bit code and 2048-bit mask.
     */
    public static IrisTemplate extractIrisCode(int[][][] unwrappedIris) {
        int numBands = 8;
        int bandHeight = 64 / numBands;
        int width = 512;
        int coefficientsPerBand = 128;

        boolean[][] sectorMask = generateSectorMask();

        int cols = coefficientsPerBand * 2;
        IrisTemplate template = new IrisTemplate(numBands, cols);

        double[] verticalGaussian = {0.03, 0.1, 0.22, 0.3, 0.22, 0.1, 0.03, 0.0};
        double frequency = 1.0 / 16.0;
        double sigma = 0.5 * Math.PI * frequency;
        double variance = sigma * sigma;

        for (int band = 0; band < numBands; band++) {
            double[] signal1D = new double[width];
            boolean[] mask1D = new boolean[width];

            // radial averaging with gaussian window
            double bandIntensitySum = 0;
            int bandValidPixelCount = 0;

            for (int x = 0; x < width; x++) {
                double intensitySum = 0;
                int validPixelCount = 0;

                for (int y = 0; y < bandHeight; y++) {
                    int globalY = band * bandHeight + y;
                    intensitySum += unwrappedIris[globalY][x][0] * verticalGaussian[y];

                    if (sectorMask[globalY][x]) {
                        validPixelCount++;
                    }
                }
                signal1D[x] = intensitySum;
                mask1D[x] = (validPixelCount >= (bandHeight / 2));

                if (mask1D[x]) {
                    bandIntensitySum += signal1D[x];
                    bandValidPixelCount++;
                }
            }

            double meanIntensity = (bandValidPixelCount > 0) ? (bandIntensitySum / bandValidPixelCount) : 0;
            for (int x = 0; x < width; x++) {
                if (mask1D[x]) {
                    signal1D[x] -= meanIntensity;
                } else {
                    signal1D[x] = 0;
                }
            }

            // gabor wavelets
            for (int v = 0; v < coefficientsPerBand; v++) {
                int x_nu = v * 4 + 2;

                double realPart = 0;
                double imagPart = 0;
                boolean isLocationValid = mask1D[x_nu];

                int windowSize = 16;
                for (int dx = -windowSize; dx <= windowSize; dx++) {
                    int x = x_nu + dx;

                    int wrappedX = x;
                    if (wrappedX < 0) wrappedX += width;
                    if (wrappedX >= width) wrappedX -= width;

                    double intensity = signal1D[wrappedX];

                    double gaussianEnv = Math.exp(-(dx * dx) / variance);
                    double angle = 2.0 * Math.PI * frequency * dx;

                    realPart += intensity * gaussianEnv * Math.cos(angle);
                    imagPart += intensity * gaussianEnv * (-Math.sin(angle));
                }

                // translating imaginary number to bits
                boolean bit1 = (realPart >= 0);
                boolean bit2 = (imagPart >= 0);

                int colIndex1 = v * 2;
                int colIndex2 = v * 2 + 1;

                template.code[band][colIndex1] = bit1;
                template.mask[band][colIndex1] = isLocationValid;

                template.code[band][colIndex2] = bit2;
                template.mask[band][colIndex2] = isLocationValid;
            }
        }

        return template;
    }

}