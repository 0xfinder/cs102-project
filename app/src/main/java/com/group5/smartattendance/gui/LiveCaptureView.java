// Java Program to take a Snapshot from System Camera
// using OpenCV

// Importing openCV modules
//package com.opencvcamera;
package gui;

// importing swing and awt classes
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
// Importing date class of sql package
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

// Class - Swing Class
public class LiveCaptureView extends JFrame {
    // Class for the face capture menu
    static {
        // Load OpenCV native library
        System.load(
                "C:\\Users\\limgo\\Desktop\\SMU_HW\\Y2S1\\CS102_Programming_Fundementals_II\\project\\opencvLibs\\opencv_java4120.dll");
    }

    // Swing Elements
    private JLabel cameraScreen;
    private JButton btnCapture;
    private JTextField textField;
    private JLabel nameLabel;
    private JButton btnBack;

    // Start camera
    private VideoCapture capture;

    // Store image as 2D matrix
    private Mat webcamFrame;
    private Mat gray;
    private boolean clicked = false;

    // OpenCV Stuff
    private String saveFolder = "images";
    private static String cascadePath = "haarcascade_frontalface_alt.xml";
    private static CascadeClassifier faceDetector = new CascadeClassifier(cascadePath);

    public LiveCaptureView() {
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
        btnCapture = new JButton("capture");
        btnCapture.setBounds(160, 480, 80, 40);
        add(btnCapture);

        btnBack = new JButton("Back");
        btnBack.setBounds(0, 480, 80, 40);
        add(btnBack);

        // Name Element
        nameLabel = new JLabel("Name:");
        nameLabel.setBounds(320, 480, 80, 40);
        add(nameLabel);

        // Text Field Element
        textField = new JTextField("");
        textField.setBounds(320 + 60, 480, 200, 40);
        add(textField);

        // Button Event Listener
        btnCapture.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                clicked = true;
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
        while (isVisible() && capture.read(webcamFrame)) {
            // read image to matrix
            // capture.read(frame);
            if (!capture.read(webcamFrame)) {
                System.out.println("No frame captured!");
                break;
            }

            detectAndSaveFace();

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

    private void detectAndSaveFace() {
        // from FaceCropDemo.java
        gray = new Mat();
        Imgproc.cvtColor(webcamFrame, gray, Imgproc.COLOR_BGR2GRAY); // Convert color image to greyscale and assign to
        // variable

        // Detect faces
        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(gray, faces, 1.1, 3, 0, new Size(30, 30), new Size());

        Rect[] faceArray = faces.toArray();

        // Draw rectangles around detected faces
        for (Rect rect : faceArray) {
            Imgproc.rectangle(webcamFrame, new Point(rect.x, rect.y),
                    new Point(rect.x + rect.width, rect.y + rect.height),
                    new Scalar(0, 255, 0), 2);

            // Add face count text
            Imgproc.putText(webcamFrame, "Face detected",
                    new Point(rect.x, rect.y - 10),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(0, 255, 0), 2);
        }

        if (faceArray.length > 0 && clicked) { // If faces detected and button is clicked
            // Crop and save the first detected face
            Rect rect = faceArray[0];
            Mat face = gray.submat(rect);
            Mat resizedFace = new Mat();
            Imgproc.resize(face, resizedFace, new Size(200, 200));

            createFile(resizedFace); // Save Image
            clicked = false;

            // Clean up temporary Mat
            resizedFace.release();
            face.release();
        }
    }

    private void createFile(Mat imageToSave) {
        // Get name from text field
        String name = textField.getText();

        // Blank name
        if (name.isBlank()) {
            name = "unnamed";
        }

        // Create folder if folder doesn't exist
        try {
            Files.createDirectories(Paths.get(saveFolder + "\\" + name));
        } catch (Exception e) {
            System.out.println(e);
        }

        // Get No. images in folder already
        // e.g. If 0 files, next file will be index 0
        int imageIndex = new File(saveFolder + "\\" + name).list().length;

        // Write to file
        Imgcodecs.imwrite(
                String.format("%s\\%s\\%s_%02d.jpg", saveFolder, name, name, imageIndex),
                imageToSave);
    }

    // Main driver method
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        EventQueue.invokeLater(new Runnable() {
            // Overriding existing run() method
            @Override
            public void run() {
                final LiveCaptureView camera = new LiveCaptureView();

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