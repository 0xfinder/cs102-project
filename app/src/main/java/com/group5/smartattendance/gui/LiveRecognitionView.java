// Code by Gordon
// Java Program to take a Snapshot from System Camera
// using OpenCV

// Importing openCV modules
//package com.opencvcamera;
package com.group5.smartattendance.gui;

// importing swing and awt classes
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
// Importing date class of sql package
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.util.Objects;
import com.group5.smartattendance.gui.*;
import com.group5.smartattendance.student.FaceData;;

// Class - Swing Class
public class LiveRecognitionView extends JFrame {
    // Swing Elements
    private JLabel cameraScreen;
    private JButton btnMarkAttendance;
    private JLabel nameLabel;
    private JButton btnBack;

    // Start camera
    private VideoCapture capture;

    // Store image as 2D matrix
    private Mat webcamFrame;
    private Mat gray;

    private String detectedName = "unknown";

    // OpenCV Stuff
    private String saveFolder = "images";
    // private static String cascadePath = Objects
    // .requireNonNull(LiveRecognitionView.class.getClassLoader()
    // .getResource("src\\main\\resources\\haarcascade_frontalface_alt.xml"))
    // .getPath();
    private static String cascadePath = "src\\main\\resources\\haarcascade_frontalface_alt.xml";
    private static CascadeClassifier faceDetector = new CascadeClassifier(cascadePath);

    public LiveRecognitionView() {
        // Load face detector
        if (faceDetector.empty()) {
            System.out.println("Error loading cascade file: " + cascadePath);
            return;
        }

        // Designing UI
        setLayout(null);

        // JFrame Title
        this.setTitle("Student Registration System");

        // Webcam Screen
        cameraScreen = new JLabel();
        cameraScreen.setBounds(0, 0, 640, 480); // x (horizontal), y (vertical), width, height
        add(cameraScreen);

        // Button Element
        btnMarkAttendance = new JButton("Mark Attendence");
        btnMarkAttendance.setBounds(320 - 160, 480, 160, 40);
        add(btnMarkAttendance);

        btnBack = new JButton("Back");
        btnBack.setBounds(0, 480, 80, 40);
        add(btnBack);

        // Name Element
        nameLabel = new JLabel("Detected Student: " + detectedName);
        nameLabel.setBounds(640 - 240, 480, 240, 40);
        add(nameLabel);

        // Button Event Listener
        btnMarkAttendance.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                markAttendance();
            }
        });

        btnBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                dispose(); // dispose method (of JFrame) kills the instance
            }
        });

        setSize(new Dimension(640, 560));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    // Creating a camera
    public void startCamera() {
        // Start Webcam
        capture = new VideoCapture(0);
        if (!capture.isOpened()) {
            System.out.println("Error opening webcam!");
            return;
        }
        webcamFrame = new Mat();
        byte[] imageData;

        ImageIcon icon;
        while (true) {
            // read image to matrix
            // capture.read(frame);
            if (!capture.read(webcamFrame)) {
                System.out.println("No frame captured!");
                break;
            }

            detectAndClassifyFace();

            // convert matrix to byte
            final MatOfByte buf = new MatOfByte();
            Imgcodecs.imencode(".jpg", webcamFrame, buf);
            imageData = buf.toArray();

            // Add to JLabel
            icon = new ImageIcon(imageData);
            cameraScreen.setIcon(icon); // Update cameraScreen with Webcam output
        }
        capture.release();
    }

    private void detectAndClassifyFace() {
        // from FaceRecognitionDemo.java
        gray = new Mat();

        JLabel label = new JLabel();
        add(label);

        Imgproc.cvtColor(webcamFrame, gray, Imgproc.COLOR_BGR2GRAY); // Convert color image to greyscale and assign to
        // variable

        // Detect faces
        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(gray, faces, 1.1, 3, 0, new Size(30, 30), new Size());

        // Rect[] faceArray = faces.toArray();

        // init detectedName to "No Student Detected" if no faces are detected
        detectedName = "No Student Detected";
        for (Rect rect : faces.toArray()) {
            // Draw rectangle
            Imgproc.rectangle(webcamFrame, new Point(rect.x, rect.y),
                    new Point(rect.x + rect.width, rect.y + rect.height),
                    new Scalar(0, 255, 0), 2);

            // Crop and resize face
            Mat face = gray.submat(rect);
            Imgproc.resize(face, face, new Size(200, 200));
            Mat faceHist = computeHistogram(face);

            // Get FaceData of all students in the dataset
            List<FaceData> studentFaceData = getFaceData();

            // Compare with training histograms
            detectedName = computeBestChoice(faceHist, studentFaceData);

            // Label based on best score (correlation: higher is better)
            Imgproc.putText(webcamFrame, detectedName, new Point(rect.x, rect.y - 10),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.9, new Scalar(0, 255, 0), 2);

        }

        // Update (other) label text
        nameLabel.setText("Detected Student: " + detectedName);

        // Display frame
        BufferedImage image = matToBufferedImage(webcamFrame);
        label.setIcon(new ImageIcon(image));
        label.repaint();
    }

    private static Mat computeHistogram(Mat image) {
        // from faceRecognitionDemo.java
        // Compute histogram for a single image
        Mat hist = new Mat();
        MatOfInt histSize = new MatOfInt(256);
        MatOfFloat ranges = new MatOfFloat(0f, 256f);
        MatOfInt channels = new MatOfInt(0);
        Imgproc.calcHist(List.of(image), channels, new Mat(), hist, histSize, ranges);
        Core.normalize(hist, hist, 0, 1, Core.NORM_MINMAX);
        return hist;
    }

    private static BufferedImage matToBufferedImage(Mat mat) {
        // from faceRecognitionDemo.java
        // Convert Mat to BufferedImage for display
        int width = mat.cols();
        int height = mat.rows();
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (mat.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
            // Convert BGR to RGB
            Mat rgbMat = new Mat();
            Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_BGR2RGB);
            mat = rgbMat;
        }

        BufferedImage image = new BufferedImage(width, height, type);
        byte[] data = new byte[width * height * (int) mat.elemSize()];
        mat.get(0, 0, data);
        image.getRaster().setDataElements(0, 0, width, height, data);
        return image;
    }

    private List<FaceData> getFaceData() {
        List<FaceData> studentFaceData = new ArrayList<>();

        File folder = new File(saveFolder);
        if (!folder.exists() || !folder.isDirectory()) {
            return studentFaceData;
        }

        String[] studentsNames = folder.list();
        if (studentsNames != null) {
            for (String name : studentsNames) {
                if (!name.equals("unnamed")) {
                    studentFaceData.add(createFaceData(name));
                }
            }
        }

        return studentFaceData;
    }

    private FaceData createFaceData(String studentName) {
        // use paths for cross platform support
        Path studentFolder = Paths.get(saveFolder, studentName);
        FaceData fd = new FaceData(studentFolder.toString());
        fd.setStudentName(studentName);
        return fd;
    }

    private String computeBestChoice(Mat faceHist, List<FaceData> studentFaceData) {
        double highestScore = 0.0;
        String highestScoreName = "unknown";
        final double threshold = 0.7; // if unable to hit threshold, get more traning data

        for (FaceData studentFD : studentFaceData) {
            double currScore = studentFD.getBestHistogramScore(faceHist);
            // debug
            // System.out.println(studentFD.getStudentName() + " " + currScore);
            if (currScore > threshold && currScore > highestScore) {
                highestScore = currScore;
                highestScoreName = studentFD.getStudentName();
            }
        }

        return highestScoreName;
    }

    // To Update
    private void markAttendance() {
        if (detectedName.equals("No Student Detected!")) {
            System.out.println("No Face Detected!");
        } else {
            System.out.printf("Attendence Marked for %s%n", detectedName);
        }
    }

    // Main driver method
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        EventQueue.invokeLater(new Runnable() {
            // Overriding existing run() method
            @Override
            public void run() {
                final LiveRecognitionView camera = new LiveRecognitionView();

                // Start camera in thread
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        camera.startCamera();
                    }
                }).start();
            }
        });
    }
}