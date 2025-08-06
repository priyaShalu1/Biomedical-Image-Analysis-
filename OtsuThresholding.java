import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.*;

public class OtsuThresholding2 {
    private static BufferedImage originalImage;
    private static BufferedImage thresholdedImage;
    private static BufferedImage foregroundImage;
    private static BufferedImage backgroundImage;
    private static JLabel probabilityLabel;

    public static void main(String[] args) {
        // Create the main frame
        JFrame frame = new JFrame("Biomedical Image Analysis");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        // Create a panel for controls
        JPanel controlPanel = new JPanel(new FlowLayout());
        JButton selectFolderButton = new JButton("Select Folder");
        JButton processImagesButton = new JButton("Process Images");
        JButton saveResultsButton = new JButton("Save Results");
        JButton exitButton = new JButton("Exit");
        controlPanel.add(selectFolderButton);
        controlPanel.add(processImagesButton);
        controlPanel.add(saveResultsButton);
        controlPanel.add(exitButton);

        // Create a panel to display images
        JPanel displayPanel = new JPanel();
        displayPanel.setLayout(new GridLayout(3, 2, 10, 10));
        JLabel originalImageLabel = new JLabel("Original Image", JLabel.CENTER);
        JLabel thresholdedImageLabel = new JLabel("Thresholded Image", JLabel.CENTER);
        JLabel foregroundImageLabel = new JLabel("Foreground Image", JLabel.CENTER);
        JLabel backgroundImageLabel = new JLabel("Background Image", JLabel.CENTER);
        probabilityLabel = new JLabel("Probability of Affection: ", JLabel.CENTER);

        displayPanel.add(originalImageLabel);
        displayPanel.add(thresholdedImageLabel);
        displayPanel.add(foregroundImageLabel);
        displayPanel.add(backgroundImageLabel);
        displayPanel.add(probabilityLabel);

        // Add panels to the frame
        frame.setLayout(new BorderLayout());
        frame.add(new JLabel("Biomedical Image Analysis", JLabel.CENTER), BorderLayout.NORTH);
        frame.add(controlPanel, BorderLayout.SOUTH);
        frame.add(displayPanel, BorderLayout.CENTER);

        // Add functionality for buttons
        final File[] selectedFolder = {null}; // To store the selected folder

        // Folder selection button
        selectFolderButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = fileChooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFolder[0] = fileChooser.getSelectedFile();
                JOptionPane.showMessageDialog(frame, "Selected Folder: " + selectedFolder[0].getAbsolutePath());
            }
        });

        // Process images button
        processImagesButton.addActionListener(e -> {
            if (selectedFolder[0] == null) {
                JOptionPane.showMessageDialog(frame, "Please select a folder first!");
                return;
            }
            processImages(selectedFolder[0], originalImageLabel, thresholdedImageLabel, foregroundImageLabel, backgroundImageLabel);
        });

        // Save results button
        saveResultsButton.addActionListener(e -> {
            if (originalImage == null) {
                JOptionPane.showMessageDialog(frame, "Please process images first before saving results!");
                return;
            }
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = fileChooser.showSaveDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File saveFolder = fileChooser.getSelectedFile();
                saveResults(saveFolder);
            }
        });

        // Exit button
        exitButton.addActionListener(e -> System.exit(0));

        // Show the frame
        frame.setVisible(true);
    }

    private static void processImages(File folder, JLabel originalLabel, JLabel thresholdedLabel, JLabel foregroundLabel, JLabel backgroundLabel) {
        try {
            File[] files = folder.listFiles();
            if (files == null || files.length == 0) {
                JOptionPane.showMessageDialog(null, "No images found in the selected folder!");
                return;
            }

            for (File file : files) {
                if (isImageFile(file)) {
                    originalImage = ImageIO.read(file);
                    BufferedImage resizedImage = resizeImage(originalImage, 256, 256);

                    BufferedImage grayscaleImage = new BufferedImage(resizedImage.getWidth(), resizedImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
                    Graphics g = grayscaleImage.getGraphics();
                    g.drawImage(resizedImage, 0, 0, null);
                    g.dispose();

                    int[] histogram = new int[256];
                    int[] cdf = new int[256];
                    computeHistogramAndCDF(grayscaleImage, histogram, cdf);

                    thresholdedImage = applyOtsuThreshold(grayscaleImage, histogram);
                    int threshold = getOtsuThreshold(histogram, resizedImage.getWidth() * resizedImage.getHeight());
                    foregroundImage = createForegroundImage(grayscaleImage, threshold);
                    backgroundImage = createBackgroundImage(grayscaleImage, threshold);

                    // Calculate probability of affection
                    double probability = calculateProbabilityOfAffection(grayscaleImage, threshold);
                    probabilityLabel.setText("Probability of Affection: " + String.format("%.2f%%", probability * 100));

                    // Update labels with images
                    originalLabel.setIcon(new ImageIcon(resizedImage));
                    thresholdedLabel.setIcon(new ImageIcon(thresholdedImage));
                    foregroundLabel.setIcon(new ImageIcon(foregroundImage));
                    backgroundLabel.setIcon(new ImageIcon(backgroundImage));

                    break; // Process the first valid image only for display
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error processing images: " + e.getMessage());
        }
    }

    private static double calculateProbabilityOfAffection(BufferedImage image, int threshold) {
        int width = image.getWidth();
        int height = image.getHeight();
        int totalPixels = width * height;
        int affectedPixels = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRaster().getSample(x, y, 0);
                if (pixel > threshold) {
                    affectedPixels++;
                }
            }
        }
        return (double) affectedPixels / totalPixels;
    }

    private static void saveResults(File folder) {
        try {
            // Create the folder if it doesn't exist
            if (!folder.exists()) {
                folder.mkdirs();
            }

            // Save processed images
            File originalImageFile = new File(folder, "original_image.png");
            ImageIO.write(originalImage, "PNG", originalImageFile);

            File thresholdedImageFile = new File(folder, "thresholded_image.png");
            ImageIO.write(thresholdedImage, "PNG", thresholdedImageFile);

            File foregroundImageFile = new File(folder, "foreground_image.png");
            ImageIO.write(foregroundImage, "PNG", foregroundImageFile);

            File backgroundImageFile = new File(folder, "background_image.png");
            ImageIO.write(backgroundImage, "PNG", backgroundImageFile);

            // Save a log file with processing details
            File logFile = new File(folder, "processing_log.txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile))) {
                writer.write("Biomedical Image Analysis Processing Log\n");
                writer.write("=====================================\n");
                writer.write("Processed images saved to folder: " + folder.getAbsolutePath() + "\n");
                writer.write("Original image: " + originalImageFile.getName() + "\n");
                writer.write("Thresholded image: " + thresholdedImageFile.getName() + "\n");
                writer.write("Foreground image: " + foregroundImageFile.getName() + "\n");
                writer.write("Background image: " + backgroundImageFile.getName() + "\n");
                writer.write("Probability of Affection: " + probabilityLabel.getText() + "\n");
                writer.write("Processing completed successfully.\n");
            }

            JOptionPane.showMessageDialog(null, "Results saved to: " + folder.getAbsolutePath());

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error saving results: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean isImageFile(File file) {
        try {
            BufferedImage img = ImageIO.read(file);
            return img != null;
        } catch (IOException e) {
            return false;
        }
    }

    private static BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
        Image tmp = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return resized;
    }

    private static void computeHistogramAndCDF(BufferedImage image, int[] histogram, int[] cdf) {
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRaster().getSample(x, y, 0);
                histogram[pixel]++;
            }
        }

        cdf[0] = histogram[0];
        for (int i = 1; i < 256; i++) {
            cdf[i] = cdf[i - 1] + histogram[i];
        }
    }

    private static BufferedImage applyOtsuThreshold(BufferedImage image, int[] histogram) {
        int width = image.getWidth();
        int height = image.getHeight();
        int threshold = getOtsuThreshold(histogram, width * height);

        BufferedImage binaryImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRaster().getSample(x, y, 0);
                int newPixel = pixel > threshold ? 255 : 0;
                binaryImage.getRaster().setSample(x, y, 0, newPixel);
            }
        }
        return binaryImage;
    }

    private static int getOtsuThreshold(int[] histogram, int totalPixels) {
        float sum = 0;
        for (int i = 0; i < 256; i++) sum += i * histogram[i];

        float sumB = 0;
        int wB = 0, threshold = 0;
        float varMax = 0;

        for (int t = 0; t < 256; t++) {
            wB += histogram[t];
            if (wB == 0) continue;

            int wF = totalPixels - wB;
            if (wF == 0) break;

            sumB += (float) (t * histogram[t]);
            float mB = sumB / wB;
            float mF = (sum - sumB) / wF;
            float varBetween = (float) wB * (float) wF * (mB - mF) * (mB - mF);

            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = t;
            }
        }
        return threshold;
    }

    private static BufferedImage createForegroundImage(BufferedImage image, int threshold) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage foregroundImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRaster().getSample(x, y, 0);
                int newPixel = pixel > threshold ? 255 : 0;
                foregroundImage.getRaster().setSample(x, y, 0, newPixel);
            }
        }
        return foregroundImage;
    }

    private static BufferedImage createBackgroundImage(BufferedImage image, int threshold) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage backgroundImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRaster().getSample(x, y, 0);
                int newPixel = pixel <= threshold ? 255 : 0;
                backgroundImage.getRaster().setSample(x, y, 0, newPixel);
            }
        }
        return backgroundImage;
    }
}
