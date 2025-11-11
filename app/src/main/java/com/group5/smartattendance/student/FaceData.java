// Code by Gordon
package com.group5.smartattendance.student;

import java.util.List;
import java.io.File;
import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class FaceData {
    private List<Mat> faceImages = new ArrayList<>();
    private List<Mat> histograms = new ArrayList<>();

    // Temporary until Student Class is finished
    private String studentId;

    public FaceData(String imageDir) {
        // from faceRecognitionDemo.java
        // Load images from a directory
        File dir = new File(imageDir);
        File[] files = dir
                .listFiles((d, name) -> name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png"));
        if (files != null) {
            for (File file : files) {
                Mat img = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
                if (!img.empty()) {
                    Imgproc.resize(img, img, new Size(200, 200));
                    faceImages.add(img);
                } else {
                    System.out.println("Failed to load image: " + file.getAbsolutePath());
                }
            }
        }

        // Compute Histograms
        computeHistograms();
    }

    private void computeHistograms() {
        // from faceRecognitionDemo.java
        // Compute histograms for a list of images
        for (Mat img : faceImages) {
            histograms.add(computeHistogram(img));
        }
    }

    private static Mat computeHistogram(Mat image) {
        // Equalize image
        Imgproc.equalizeHist(image, image);
        // from faceRecognitionDemo.java
        // Compute histogram for a single image
        Mat hist = new Mat();
        MatOfInt histSize = new MatOfInt(256);
        MatOfFloat ranges = new MatOfFloat(0f, 256f);
        MatOfInt channels = new MatOfInt(0);
        Imgproc.calcHist(List.of(image), channels, new Mat(), hist, histSize, ranges);
        // Equalize Histogram Data
        // hist.convertTo(hist, 0); // Convert from CV_32FC1 to CV_8UC1
        // Imgproc.equalizeHist(hist, hist);
        // hist.convertTo(hist, 5); // Convert from CV_8UC1 to CV_32FC1
        Core.normalize(hist, hist, 0, 1, Core.NORM_MINMAX);
        return hist;
    }

    public double getBestHistogramScore(Mat faceHist) {
        // from faceRecognitionDemo.java
        // Get best histogram comparison score
        double bestScore = 0;
        for (Mat hist : histograms) {
            double score = Imgproc.compareHist(faceHist, hist, Imgproc.HISTCMP_CORREL);
            bestScore = Math.max(bestScore, score);
        }
        return bestScore;
    }

    public List<Mat> getFaceImages() {
        return faceImages;
    }

    public List<Mat> getHistograms() {
        return histograms;
    }

    public String getStudentID() {
        return studentId;
    }

    public void setStudentID(String sid) {
        studentId = sid;
    }
}
