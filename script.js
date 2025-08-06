document.addEventListener('DOMContentLoaded', () => {
    const imageUpload = document.getElementById('imageUpload');
    const processButton = document.getElementById('processButton');
    const saveButton = document.getElementById('saveButton');
    const probabilityLabel = document.getElementById('probabilityLabel');
    const errorMessage = document.getElementById('errorMessage');

    const originalCanvas = document.getElementById('originalCanvas');
    const thresholdedCanvas = document.getElementById('thresholdedCanvas');
    const foregroundCanvas = document.getElementById('foregroundCanvas');
    const backgroundCanvas = document.getElementById('backgroundCanvas');

    const originalCtx = originalCanvas.getContext('2d');
    const thresholdedCtx = thresholdedCanvas.getContext('2d');
    const foregroundCtx = foregroundCanvas.getContext('2d');
    const backgroundCtx = backgroundCanvas.getContext('2d');

    const placeholderTexts = document.querySelectorAll('.placeholder-text');

    let originalImage = null;
    const IMAGE_SIZE = 256; // Standard size for processing

    // --- Event Listeners ---
    imageUpload.addEventListener('change', (event) => {
        const file = event.target.files[0];
        if (file) {
            loadImage(file);
            processButton.disabled = false;
            saveButton.disabled = true; // Disable save until processed
            errorMessage.textContent = '';
            probabilityLabel.querySelector('span').textContent = 'N/A';
            clearCanvases();
            showPlaceholders();
        } else {
            processButton.disabled = true;
            saveButton.disabled = true;
            originalImage = null;
            clearCanvases();
            showPlaceholders();
        }
    });

    processButton.addEventListener('click', () => {
        if (originalImage) {
            processImage();
        } else {
            displayError('Please select an image first.');
        }
    });

    saveButton.addEventListener('click', () => {
        if (originalImage) {
            saveResults();
        } else {
            displayError('No processed images to save. Please process an image first.');
        }
    });

    // --- Image Loading and Display ---
    function loadImage(file) {
        const reader = new FileReader();
        reader.onload = (e) => {
            const img = new Image();
            img.crossOrigin = "anonymous"; // Required for canvas.toDataURL if image is from different origin
            img.onload = () => {
                originalImage = img;
                drawResizedImage(originalCtx, originalImage, originalCanvas);
                hidePlaceholder('originalPlaceholder');
            };
            img.onerror = () => {
                displayError('Could not load image. Please ensure it is a valid image file.');
                originalImage = null;
                processButton.disabled = true;
                saveButton.disabled = true;
                showPlaceholders();
            };
            img.src = e.target.result;
        };
        reader.onerror = () => {
            displayError('Error reading file.');
            originalImage = null;
            processButton.disabled = true;
            saveButton.disabled = true;
            showPlaceholders();
        };
        reader.readAsDataURL(file);
    }

    function drawResizedImage(ctx, img, canvas) {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        ctx.drawImage(img, 0, 0, IMAGE_SIZE, IMAGE_SIZE);
    }

    function clearCanvases() {
        originalCtx.clearRect(0, 0, originalCanvas.width, originalCanvas.height);
        thresholdedCtx.clearRect(0, 0, thresholdedCanvas.width, thresholdedCanvas.height);
        foregroundCtx.clearRect(0, 0, foregroundCanvas.width, foregroundCanvas.height);
        backgroundCtx.clearRect(0, 0, backgroundCanvas.width, backgroundCanvas.height);
    }

    function showPlaceholders() {
        placeholderTexts.forEach(p => p.style.display = 'block');
    }

    function hidePlaceholder(id) {
        document.getElementById(id).style.display = 'none';
    }

    // --- Image Processing Logic (Otsu's Thresholding) ---
    function processImage() {
        errorMessage.textContent = '';
        processButton.disabled = true; // Disable during processing
        processButton.textContent = 'Processing...';

        // Get grayscale image data from the original canvas
        const imageData = originalCtx.getImageData(0, 0, IMAGE_SIZE, IMAGE_SIZE);
        const grayscaleData = convertToGrayscale(imageData);

        // Compute histogram
        const histogram = computeHistogram(grayscaleData);

        // Get Otsu's threshold
        const totalPixels = IMAGE_SIZE * IMAGE_SIZE;
        const threshold = getOtsuThreshold(histogram, totalPixels);

        // Apply threshold and draw results
        const thresholdedImageData = applyThreshold(grayscaleData, threshold, 'binary');
        thresholdedCtx.putImageData(thresholdedImageData, 0, 0);
        hidePlaceholder('thresholdedPlaceholder');

        const foregroundImageData = applyThreshold(grayscaleData, threshold, 'foreground');
        foregroundCtx.putImageData(foregroundImageData, 0, 0);
        hidePlaceholder('foregroundPlaceholder');

        const backgroundImageData = applyThreshold(grayscaleData, threshold, 'background');
        backgroundCtx.putImageData(backgroundImageData, 0, 0);
        hidePlaceholder('backgroundPlaceholder');

        // Calculate probability of affection
        const probability = calculateProbabilityOfAffection(grayscaleData, threshold);
        probabilityLabel.querySelector('span').textContent = `${(probability * 100).toFixed(2)}%`;

        processButton.disabled = false;
        processButton.textContent = 'Process Image';
        saveButton.disabled = false; // Enable save after processing
    }

    function convertToGrayscale(imageData) {
        const data = imageData.data;
        const grayscaleData = new ImageData(IMAGE_SIZE, IMAGE_SIZE);
        const grayscalePixels = grayscaleData.data;

        for (let i = 0; i < data.length; i += 4) {
            const r = data[i];
            const g = data[i + 1];
            const b = data[i + 2];
            // Luminosity method for grayscale
            const gray = Math.round(0.299 * r + 0.587 * g + 0.114 * b);
            grayscalePixels[i] = gray;     // R
            grayscalePixels[i + 1] = gray; // G
            grayscalePixels[i + 2] = gray; // B
            grayscalePixels[i + 3] = 255;  // A (alpha)
        }
        return grayscaleData;
    }

    function computeHistogram(grayscaleData) {
        const histogram = new Array(256).fill(0);
        const pixels = grayscaleData.data;
        for (let i = 0; i < pixels.length; i += 4) {
            histogram[pixels[i]]++; // Use the R channel as it's grayscale
        }
        return histogram;
    }

    function getOtsuThreshold(histogram, totalPixels) {
        let sum = 0;
        for (let i = 0; i < 256; i++) sum += i * histogram[i];

        let sumB = 0;
        let wB = 0;
        let maximumVariance = 0;
        let threshold = 0;

        for (let t = 0; t < 256; t++) {
            wB += histogram[t]; // Weight background
            if (wB === 0) continue;

            const wF = totalPixels - wB; // Weight foreground
            if (wF === 0) break;

            sumB += t * histogram[t];
            const meanB = sumB / wB; // Mean background
            const meanF = (sum - sumB) / wF; // Mean foreground

            // Calculate between-class variance
            const varianceBetween = wB * wF * (meanB - meanF) * (meanB - meanF);

            if (varianceBetween > maximumVariance) {
                maximumVariance = varianceBetween;
                threshold = t;
            }
        }
        return threshold;
    }

    function applyThreshold(grayscaleData, threshold, type) {
        const data = grayscaleData.data;
        const outputImageData = new ImageData(IMAGE_SIZE, IMAGE_SIZE);
        const outputPixels = outputImageData.data;

        for (let i = 0; i < data.length; i += 4) {
            const pixelValue = data[i]; // Grayscale value

            let newPixelValue;
            if (type === 'binary') {
                newPixelValue = pixelValue > threshold ? 255 : 0;
            } else if (type === 'foreground') {
                newPixelValue = pixelValue > threshold ? pixelValue : 0; // Keep original value if foreground, else black
            } else { // background
                newPixelValue = pixelValue <= threshold ? pixelValue : 0; // Keep original value if background, else black
            }

            outputPixels[i] = newPixelValue;
            outputPixels[i + 1] = newPixelValue;
            outputPixels[i + 2] = newPixelValue;
            outputPixels[i + 3] = 255; // Alpha
        }
        return outputImageData;
    }

    function calculateProbabilityOfAffection(grayscaleData, threshold) {
        let affectedPixels = 0;
        const pixels = grayscaleData.data;
        for (let i = 0; i < pixels.length; i += 4) {
            if (pixels[i] > threshold) {
                affectedPixels++;
            }
        }
        return affectedPixels / (pixels.length / 4); // Divide by 4 because each pixel has 4 channels (RGBA)
    }

    // --- Saving Results ---
    function saveResults() {
        const downloadCanvas = (canvas, filename) => {
            const link = document.createElement('a');
            link.download = filename;
            link.href = canvas.toDataURL('image/png');
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        };

        downloadCanvas(originalCanvas, 'original_image.png');
        downloadCanvas(thresholdedCanvas, 'thresholded_image.png');
        downloadCanvas(foregroundCanvas, 'foreground_image.png');
        downloadCanvas(backgroundCanvas, 'background_image.png');

        alert('Results saved to your downloads!');
    }

    // --- Utility for Error Display ---
    function displayError(message) {
        errorMessage.textContent = message;
        console.error(message);
    }
});
