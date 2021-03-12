package com.example.imageclassifier.classifier;

import android.graphics.Bitmap;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ImageClassifier {

    // Quantized MobileNet models requires additional dequantization to the output probability
    private static final float PROBABILITY_MEAN = 0.0f;
    private static final float PROBABILITY_STD = 255.0f;

    //The quantized model does not require normalization, thus set mean as 0.0f, and std as 1.0f to bypass the normalization.
    private static final float image_STD = 1.0f;
    private static final float image_MEAN = 0.0f;

    private static final int maxSize = 5;

    private final int imageResizeX;
    private final int imageResizeY;

    private final Interpreter tensorClassifier;
    private TensorImage inputImageBuffer;
    private final TensorBuffer probabilityImageBuffer;
    private final TensorProcessor probabilityProcessor;
    private List <String> labels;

    public ImageClassifier(MappedByteBuffer classifierModel, List <String> labels) {
        this.labels = labels;
        tensorClassifier = new Interpreter(classifierModel, null);

        int imageTensorIndex = 0; // input
        int probabilityTensorIndex = 0;// output

        int[] inputImageShape = tensorClassifier.getInputTensor(imageTensorIndex).shape();
        DataType inputDataType = tensorClassifier.getInputTensor(imageTensorIndex).dataType();

        int[] outputImageShape = tensorClassifier.getOutputTensor(probabilityTensorIndex).shape();
        DataType outputDataType = tensorClassifier.getOutputTensor(probabilityTensorIndex).dataType();

        imageResizeX = inputImageShape[1];
        imageResizeY = inputImageShape[2];

        // Creates the input tensor.
        inputImageBuffer = new TensorImage(inputDataType);

        // Creates the output tensor and its processor.
        probabilityImageBuffer = TensorBuffer.createFixedSize(outputImageShape, outputDataType);

        // Creates the post processor for the output probability.
        probabilityProcessor = new TensorProcessor.Builder().add(new NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD))
                .build();
    }

    /**
     * method runs the inference and returns the classification results
     *
     * @param bitmap            the bitmap of the image
     * @param sensorOrientation orientation of the camera
     * @return classification results
     */
    public List<Recognition> recognizeImage(final Bitmap bitmap, final int sensorOrientation) {
        List<Recognition> recognitions = new ArrayList<>();

        inputImageBuffer = loadImage(bitmap, sensorOrientation);
        tensorClassifier.run(inputImageBuffer.getBuffer(), probabilityImageBuffer.getBuffer().rewind());

        // Gets the map of label and probability.
        Map<String, Float> labelledProbability = new TensorLabel(labels,
                probabilityProcessor.process(probabilityImageBuffer)).getMapWithFloatValue();

        for (Map.Entry<String, Float> entry : labelledProbability.entrySet()) {
            recognitions.add(new Recognition(entry.getKey(), entry.getValue()));
        }

        // Find the best classifications by sorting predicitons based on confidence
        Collections.sort(recognitions);

        // returning top 5 predicitons
        return recognitions.subList(0, maxSize);
    }


    private TensorImage loadImage(Bitmap bitmap, int sensorOrientation) {
        // Loads bitmap into a TensorImage.
        inputImageBuffer.load(bitmap);

        int noOfRotations = sensorOrientation / 90;
        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());

        // Creates processor for the TensorImage.
        // pre processing steps are applied here
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                .add(new ResizeOp(imageResizeX, imageResizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                .add(new Rot90Op(noOfRotations))
                .add(new NormalizeOp(image_MEAN, image_STD))
                .build();
        return imageProcessor.process(inputImageBuffer);
    }

    //An immutable result returned by a Classifier describing what was recognized
    public class Recognition implements Comparable {

        //Display name for the recognition
        private String name;

        //A sortable score for how good the recognition is relative to others. Higher should be better
        private float confidence;

        public Recognition(String name, float confidence) {
            this.name = name;
            this.confidence = confidence;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public float getConfidence() {
            return confidence;
        }

        public void setConfidence(float confidence) {
            this.confidence = confidence;
        }

        @Override
        public String toString() {
            return "Recognition{" +
                    "name='" + name + '\'' +
                    ", confidence=" + confidence +
                    '}';
        }

        @Override
        public int compareTo(Object obj) {
            return Float.compare(((Recognition) obj).confidence, this.confidence);
        }
    }
}
