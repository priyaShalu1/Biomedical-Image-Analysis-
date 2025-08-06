# Biomedical-Image-Analysis
Java project implementing Otsu's thresholding to convert grayscale images into binary format by automatically determining the optimal threshold for image segmentation and processing.
# Otsu's Thresholding in Java
This project demonstrates the implementation of **Otsu's Binarization Algorithm** in Java to perform image thresholding. The algorithm is widely used in image processing to convert grayscale images into binary images by automatically determining the optimal threshold value.
---
## 📸 Project Overview

This Java program:
- Loads a grayscale image.
- Applies **Otsu's method** to compute the optimal threshold.
- Generates a binarized (black and white) version of the image.
- Saves the output image to disk.

Otsu’s method works by maximizing the between-class variance, effectively separating foreground and background.
---
## 📁 File Structure
OtsuThresholding.java # Java program for Otsu’s thresholding
Images.zip # Zip file containing test input images
output/ # Output directory (will be created automatically)
---
## 🛠️ Requirements
- Java Development Kit (JDK) 8 or above
- No external libraries required (uses only core Java)
---
## 🚀 How to Run
Compile the code: javac OtsuThresholding.java
Run the code: java OtsuThresholding
Give the images from images.zip file after unzipping it.
---
##Sample Result
Input image -> Grayscale -> Binarized
Binarized images will be saved in the output folder.
<img width="525" height="694" alt="Screenshot 2025-08-06 150721" src="https://github.com/user-attachments/assets/92962bdd-101b-4299-8b89-75a434a36e25" />


 

