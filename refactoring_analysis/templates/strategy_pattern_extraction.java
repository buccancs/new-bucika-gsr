// Template: Strategy Pattern for Processing Logic

// Before: Monolithic Processing Class (1700+ lines)
public class OpencvTools {
    public static void processImage(Mat image, int processingType) {
        if (processingType == TYPE_THERMAL) {
            // 400+ lines of thermal processing
        } else if (processingType == TYPE_CONTRAST) {
            // 300+ lines of contrast processing  
        } else if (processingType == TYPE_FILTER) {
            // 500+ lines of filter processing
        }
        // ... more processing types
    }
}

// After: Strategy Pattern Implementation
public interface ImageProcessingStrategy {
    Mat process(Mat inputImage, ProcessingParameters params);
}

public class ThermalProcessingStrategy implements ImageProcessingStrategy {
    @Override
    public Mat process(Mat inputImage, ProcessingParameters params) {
        // Focused thermal processing logic (400 lines)
        return processedImage;
    }
}

public class ContrastProcessingStrategy implements ImageProcessingStrategy {
    @Override  
    public Mat process(Mat inputImage, ProcessingParameters params) {
        // Focused contrast processing logic (300 lines)
        return processedImage;
    }
}

public class ImageProcessor {
    private ImageProcessingStrategy strategy;
    
    public void setStrategy(ImageProcessingStrategy strategy) {
        this.strategy = strategy;
    }
    
    public Mat processImage(Mat image, ProcessingParameters params) {
        return strategy.process(image, params);
    }
}

// Usage in main class (now much simpler)
public class OpencvTools {
    private static final ImageProcessor processor = new ImageProcessor();
    
    public static Mat processThermalImage(Mat image, ProcessingParameters params) {
        processor.setStrategy(new ThermalProcessingStrategy());
        return processor.processImage(image, params);
    }
}
