package core;

/**
 * Utility class that implements all image processing algorithms.
 */
public class ImageProcessor {

    // ==========================================================
    // PIXEL OPERATIONS
    // ==========================================================

    public enum GrayscaleOptions {
        AVERAGING,
        LUMINANCE,
        DESATURATION,
        DECOMPOSITION_MAX,
        DECOMPOSITION_MIN,
        SINGLE_RED,
        SINGLE_GREEN,
        SINGLE_BLUE,
        CUSTOM,
        CUSTOM_DITHERED
    }

    /**
     * Converts an image to grayscale using the standard conversion algorithm selected via the provided option.
     *
     * @param originalMatrix The 3D array representing the image.
     * @param option The chosen grayscale algorithm.
     * @return A new 3D array representing the grayscale image.
     */
    public static int[][][] applyGrayscale(int[][][] originalMatrix, GrayscaleOptions option) {
        if (originalMatrix == null) return null;

        int height = originalMatrix.length;
        int width = originalMatrix[0].length;
        int[][][] newMatrix = new int[height][width][3];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = originalMatrix[y][x][0];
                int g = originalMatrix[y][x][1];
                int b = originalMatrix[y][x][2];

                int gray = 0;
                int min = Math.min(r, Math.min(g, b));
                int max = Math.max(r, Math.max(g, b));

                if (option == GrayscaleOptions.AVERAGING) {
                    gray = Math.round((float) (r + g + b) / 3);
                } else if (option == GrayscaleOptions.LUMINANCE) {
                    gray = (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
                } else if (option == GrayscaleOptions.DESATURATION) {
                    gray = Math.round((float) (max + min) / 2);
                } else if (option == GrayscaleOptions.DECOMPOSITION_MAX) {
                    gray = max;
                } else if (option == GrayscaleOptions.DECOMPOSITION_MIN) {
                    gray = min;
                } else if (option == GrayscaleOptions.SINGLE_RED) {
                    gray = r;
                } else if (option == GrayscaleOptions.SINGLE_GREEN) {
                    gray = g;
                } else if (option == GrayscaleOptions.SINGLE_BLUE) {
                    gray = b;
                }

                gray = Math.min(Math.max(gray, 0), 255);

                newMatrix[y][x][0] = gray;
                newMatrix[y][x][1] = gray;
                newMatrix[y][x][2] = gray;
            }
        }

        return newMatrix;
    }

    /**
     * Converts an image to grayscale using a quantization algorithm that limits the output
     * to a specific number of shades, optionally applying dithering.
     *
     * @param originalMatrix The 3D array representing the image.
     * @param option The quantization option (Custom or Custom Dithered).
     * @param shades The number of grayscale shades to reduce the image to.
     * @return A new 3D array representing the quantized grayscale image.
     */
    public static int[][][] applyGrayscale(int[][][] originalMatrix, GrayscaleOptions option, int shades) {
        if (originalMatrix == null) return null;

        int height = originalMatrix.length;
        int width = originalMatrix[0].length;
        int[][][] newMatrix = new int[height][width][3];

        double factor = 255.0 / (shades - 1);
        int errorVal = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = originalMatrix[y][x][0];
                int g = originalMatrix[y][x][1];
                int b = originalMatrix[y][x][2];

                int gray = 0;
                int avgVal = Math.round((float) (r + g + b) / 3);

                if (option == GrayscaleOptions.CUSTOM) {
                    gray = (int) Math.round(Math.round(avgVal / factor) * factor);
                } else if (option == GrayscaleOptions.CUSTOM_DITHERED) {
                    int tempGray = avgVal + errorVal;
                    gray = (int) Math.round(Math.round(tempGray / factor) * factor);
                    errorVal += avgVal - gray;
                }

                gray = Math.min(Math.max(gray, 0), 255);

                newMatrix[y][x][0] = gray;
                newMatrix[y][x][1] = gray;
                newMatrix[y][x][2] = gray;
            }
            errorVal = 0;
        }

        return newMatrix;
    }

    // ==========================================================
    // BINARIZATION
    // ==========================================================

    /**
     * Segments an image into multiple brightness classes based on an array of thresholds.
     *
     * @param originalMatrix The 3D array representing the image.
     * @param thresholds An array of integer thresholds to split the image by.
     * @return A segmented 3D array.
     */
    public static int[][][] applySegmentation(int[][][] originalMatrix, int... thresholds) {
        if (originalMatrix == null) return null;

        int height = originalMatrix.length;
        int width = originalMatrix[0].length;
        int[][][] newMatrix = new int[height][width][3];

        java.util.Arrays.sort(thresholds);

        int classes = thresholds.length + 1;
        int[] colors = new int[classes];
        for (int i = 0; i < classes; i++) {
            colors[i] = (int) Math.round((255.0 / (classes - 1)) * i);
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int c = 0; c < 3; c++) {
                    int val = originalMatrix[y][x][c];

                    int classIdx = classes - 1;
                    for (int i = 0; i < thresholds.length; i++) {
                        if (val <= thresholds[i]) {
                            classIdx = i;
                            break;
                        }
                    }
                    newMatrix[y][x][c] = colors[classIdx];
                }
            }
        }
        return newMatrix;
    }

    /**
     * A helper function to recursively generate all possible combinations of thresholds for multi-level
     * Otsu's method.
     *
     * @param step The current depth of the recursion.
     * @param startVal The starting pixel intensity value to check.
     * @param numThresholds The total number of thresholds needed (classes - 1).
     * @param current The array holding the currently evaluated threshold combination.
     * @param best The array holding the best threshold combination found so far.
     * @param maxVar A 1-element array holding the maximum variance found so far.
     * @param P The cumulative probability array of the image histogram.
     * @param S The cumulative mean array of the image histogram.
     */
    private static void generateCombinations(int step, int startVal, int numThresholds, int[] current, int[] best, double[] maxVar, double[] P, double[] S) {
        if (step == numThresholds) {
            double currentVar = 0.0;
            int lastT = -1;

            for (int i = 0; i <= numThresholds; i++) {
                int tStart = lastT + 1;
                int tEnd = (i == numThresholds) ? 255 : current[i];
                if (tStart > tEnd) return;

                double w = P[tEnd] - (tStart > 0 ? P[tStart - 1] : 0);
                if (w > 0) {
                    double m = (S[tEnd] - (tStart > 0 ? S[tStart - 1] : 0)) / w;
                    currentVar += w * m * m;
                }
                lastT = tEnd;
            }

            if (currentVar > maxVar[0]) {
                maxVar[0] = currentVar;
                System.arraycopy(current, 0, best, 0, numThresholds);
            }
            return;
        }

        for (int t = startVal; t < 256; t++) {
            current[step] = t;
            generateCombinations(step + 1, t + 1, numThresholds, current, best, maxVar, P, S);
        }
    }

    // ==========================================================
    // CONVOLUTION FILTERS
    // ==========================================================

    /**
     * Retrieves a pixel's RGB values (applying the selected boundary logic for pixels outside the image).
     *
     * @param matrix The original 3D image array.
     * @param x The requested X coordinate.
     * @param y The requested Y coordinate.
     * @param width The width of the image.
     * @param height The height of the image.
     * @param mode The boundary handling rule (e.g., MIRROR, PAD_BLACK).
     * @return An integer array [R, G, B] of the fetched pixel.
     */
    private static int[] getPixelWithBoundary(int[][][] matrix, int x, int y, int width, int height, OptionPanel.BoundaryMode mode) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return matrix[y][x];
        }

        switch (mode) {
            case PAD_BLACK: return new int[]{0, 0, 0};
            case PAD_WHITE: return new int[]{255, 255, 255};
            case PAD_GRAY:  return new int[]{128, 128, 128};
            case MIRROR:
                int mx = x;
                if (mx < 0) mx = Math.abs(mx);
                if (mx >= width) mx = 2 * width - mx - 2;
                mx = Math.max(0, Math.min(mx, width - 1));

                int my = y;
                if (my < 0) my = Math.abs(my);
                if (my >= height) my = 2 * height - my - 2;
                my = Math.max(0, Math.min(my, height - 1));

                return matrix[my][mx];
            case REPLICATE:
            case CROP:
            case KEEP_ORIGINAL:
            default:
                int sx = Math.max(0, Math.min(x, width - 1));
                int sy = Math.max(0, Math.min(y, height - 1));
                return matrix[sy][sx];
        }
    }

    // ==========================================================
    // RGB & HSV
    // ==========================================================

    /**
     * Converts an RGB color to the HSV color space.
     *
     * @return A double array [Hue (0-360), Saturation (0-1), Value (0-255)].
     */
    public static double[] rgbToHsv(int r, int g, int b) {
        double rNorm = r / 255.0;
        double gNorm = g / 255.0;
        double bNorm = b / 255.0;

        double cMax = Math.max(rNorm, Math.max(gNorm, bNorm));
        double cMin = Math.min(rNorm, Math.min(gNorm, bNorm));
        double delta = cMax - cMin;

        double h = 0;
        double s = 0;
        double v = cMax * 255.0;

        if (delta == 0) {
            h = 0;
        } else if (cMax == rNorm) {
            h = 60 * (((gNorm - bNorm) / delta) % 6);
        } else if (cMax == gNorm) {
            h = 60 * (((bNorm - rNorm) / delta) + 2);
        } else if (cMax == bNorm) {
            h = 60 * (((rNorm - gNorm) / delta) + 4);
        }

        if (h < 0) {
            h += 360;
        }

        if (cMax > 0) {
            s = delta / cMax;
        } else {
            s = 0;
        }

        return new double[]{h, s, v};
    }

    /**
     * Converts an HSV color back to the RGB color space.
     *
     * @return An integer array [R, G, B] constrained to 0-255.
     */
    public static int[] hsvToRgb(double h, double s, double v) {
        double vNorm = v / 255.0;

        double c = vNorm * s;
        double x = c * (1 - Math.abs((h / 60.0) % 2 - 1));
        double m = vNorm - c;

        double rPrime = 0, gPrime = 0, bPrime = 0;

        if (h >= 0 && h < 60) {
            rPrime = c; gPrime = x; bPrime = 0;
        } else if (h >= 60 && h < 120) {
            rPrime = x; gPrime = c; bPrime = 0;
        } else if (h >= 120 && h < 180) {
            rPrime = 0; gPrime = c; bPrime = x;
        } else if (h >= 180 && h < 240) {
            rPrime = 0; gPrime = x; bPrime = c;
        } else if (h >= 240 && h < 300) {
            rPrime = x; gPrime = 0; bPrime = c;
        } else if (h >= 300 && h < 360) {
            rPrime = c; gPrime = 0; bPrime = x;
        }

        int r = (int) Math.round((rPrime + m) * 255);
        int g = (int) Math.round((gPrime + m) * 255);
        int b = (int) Math.round((bPrime + m) * 255);

        r = Math.min(Math.max(r, 0), 255);
        g = Math.min(Math.max(g, 0), 255);
        b = Math.min(Math.max(b, 0), 255);

        return new int[]{r, g, b};
    }

    // ==========================================================
    // Projections
    // ==========================================================

    /**
     * Calculates the horizontal and vertical projections of black pixels in an image.
     *
     * @param originalMatrix The 3D array representing the image.
     * @return A 2D array containing [verticalProjectionArray, horizontalProjectionArray].
     */
    public static int[][] getProjections(int[][][] originalMatrix) {
        if (originalMatrix == null) return null;

        int height = originalMatrix.length;
        int width = originalMatrix[0].length;

        int[] verticalProj = new int[width];
        int[] horizontalProj = new int[height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (originalMatrix[y][x][0] == 0) {
                    verticalProj[x]++;
                    horizontalProj[y]++;
                }
            }
        }

        return new int[][] {verticalProj, horizontalProj};
    }

    // ==========================================================
    // MORPHOLOGY OPERATIONS
    // ==========================================================

    /**
     * Applies a morphological operation (erosion or dilation) to the image based on the specified structuring element.
     *
     * @param originalMatrix The 3D array representing the image.
     * @param structuralElement The 2D boolean array representing the active cells of the structuring element.
     * @param mode The boundary handling rule for edges.
     * @param min A boolean flag determining the operation type: true to perform erosion (calculating the local minimum),
     *           or false to perform dilation (calculating the local maximum).
     * @return A new 3D array representing the modified color image.
     */
    private static int[][][] applyMorphology(int[][][] originalMatrix, boolean[][] structuralElement, OptionPanel.BoundaryMode mode, boolean min) {
        if (originalMatrix == null) return null;

        int height = originalMatrix.length;
        int width = originalMatrix[0].length;
        int[][][] newMatrix = new int[height][width][3];

        int windowSize = structuralElement.length;
        int offset = windowSize / 2;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int minVal = 255;
                int maxVal = 0;

                for (int wy = -offset; wy <= offset; wy++) {
                    for (int wx = -offset; wx <= offset; wx++) {

                        if (structuralElement[wy + offset][wx + offset]) {
                            int px = x + wx;
                            int py = y + wy;

                            int[] rgb = getPixelWithBoundary(originalMatrix, px, py, width, height, mode);
                            int v = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));

                            if (v < minVal) {
                                minVal = v;
                            }
                            if (v > maxVal) {
                                maxVal = v;
                            }
                        }
                    }
                }

                int r = originalMatrix[y][x][0];
                int g = originalMatrix[y][x][1];
                int b = originalMatrix[y][x][2];
                double[] hsv = rgbToHsv(r, g, b);

                if (min) {
                    newMatrix[y][x] = hsvToRgb(hsv[0], hsv[1], minVal);
                } else {
                    newMatrix[y][x] = hsvToRgb(hsv[0], hsv[1], maxVal);
                }

            }
        }

        return newMatrix;
    }

    /**
     * Applies erosion to the image based on the specified structuring element.
     *
     * @param originalMatrix The 3D array representing the image.
     * @param structuralElement The 2D boolean array representing the active cells of the structuring element.
     * @param mode The boundary handling rule for edges.
     * @return A new 3D array representing the modified color image.
     */
    public static int[][][] applyErosion(int[][][] originalMatrix, boolean[][] structuralElement, OptionPanel.BoundaryMode mode) {
        return applyMorphology(originalMatrix, structuralElement, mode, true);
    }

    /**
     * Applies dilation to the image based on the specified structuring element.
     *
     * @param originalMatrix The 3D array representing the image.
     * @param structuralElement The 2D boolean array representing the active cells of the structuring element.
     * @param mode The boundary handling rule for edges.
     * @return A new 3D array representing the modified color image.
     */
    public static int[][][] applyDilation(int[][][] originalMatrix, boolean[][] structuralElement, OptionPanel.BoundaryMode mode) {
        return applyMorphology(originalMatrix, structuralElement, mode, false);
    }

    public static int[][][] applyOpening(int[][][] originalMatrix, boolean[][] structuralElement, OptionPanel.BoundaryMode mode) {
        int [][][] temp = applyErosion(originalMatrix, structuralElement, mode);
        return applyDilation(temp, structuralElement, mode);
    }

    public static int[][][] applyClosing(int[][][] originalMatrix, boolean[][] structuralElement, OptionPanel.BoundaryMode mode) {
        int [][][] temp = applyDilation(originalMatrix, structuralElement, mode);
        return applyErosion(temp, structuralElement, mode);
    }


}
