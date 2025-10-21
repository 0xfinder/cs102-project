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
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
// Importing date class of sql package
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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

import com.group5.smartattendance.core.CascadeLoader;
import com.group5.smartattendance.persistence.StudentManager;
import com.group5.smartattendance.student.Student;

// Class - Swing Class
public class LiveCaptureView extends JFrame {
    // Swing Elements
    private JLabel cameraScreen;
    private JButton btnCapture;
    private JButton btnBack;

    // private JCheckBox newStudentCheck;
    // private JLabel newStudentLabel;

    // private JLabel nameLabel;
    // private JTextField nameTextField;
    // private JLabel sidFromNameLabel;
    private JButton btnRegisterNewStudent;

    private JLabel sidLabel;
    private JTextField sidTextField;
    private JLabel nameFromSIDLabel;

    // Start camera
    private VideoCapture capture;

    // Store image as 2D matrix
    private Mat webcamFrame;
    private Mat gray;
    private boolean clicked = false;

    // OpenCV Stuff
    private String saveFolder = "images";
    private static CascadeClassifier faceDetector = CascadeLoader.loadDefaultFaceCascade();

    public LiveCaptureView() {
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

        // Is New Student Checkbox
        // newStudentCheck = new JCheckBox();
        // newStudentCheck.setBounds(320, 480, 40, 40);
        // add(newStudentCheck);

        // newStudentLabel = new JLabel("New Student");
        // newStudentLabel.setBounds(360, 480, 80, 40);
        // add(newStudentLabel);

        // NEW STUDENT UI
        // Name Input, SID will be assigned and displayed after checking DB
        // Name Element

        btnRegisterNewStudent = new JButton("Register New Student");
        btnRegisterNewStudent.setBounds(320, 480, 240, 40);
        add(btnRegisterNewStudent);

        // EXISTING STUDENT UI
        // SID Input (case-sensitive), Name will be displayed if SID exists in DB,
        // else "Not found"
        sidLabel = new JLabel("SID:");
        sidLabel.setBounds(320, 520, 80, 40);
        add(sidLabel);

        // Text Field Element
        sidTextField = new JTextField("");
        sidTextField.setBounds(320 + 60, 520, 200, 40);
        add(sidTextField);

        nameFromSIDLabel = new JLabel("Please enter an SID");
        nameFromSIDLabel.setBounds(320, 560, 80, 40);
        add(nameFromSIDLabel);

        // Init button event listeners
        buttonEvents();

        setSize(new Dimension(640, 640));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void buttonEvents() {
        // Button Event Listener
        // Capture button click
        btnCapture.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clicked = true;
            }
        });

        // Back Button
        btnBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                dispose(); // dispose method (of JFrame) kills the instance
            }
        });

        // Checkbox update
        btnRegisterNewStudent.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Open new view
                System.out.println("Register Student View"); // dubug
                new RegisterStudentView();
            }
        });

        // On Text Field Enter
        sidTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // update label text
                String tempSid = sidTextField.getText();
                String studentName = getNameFromSID(tempSid);
                if (studentName.isEmpty()) {
                    nameFromSIDLabel.setText("Invalid SID");
                } else {
                    nameFromSIDLabel.setText("Student: " + studentName);
                }
            }
        });
    }

    private String getNameFromSID(String sid) {
        try {
            Optional<Student> student = StudentManager.findById(sid);
            if (student.isPresent()) {
                return student.get().getName();
            } else {
                return "";
            }
        } catch (Exception e) {
            return "";
        }
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
        String sid = sidTextField.getText(); // To Replace with SID

        // Blank name
        if (sid.isBlank()) {
            sid = "unnamed";
        }

        // Create folder if folder doesn't exist
        Path studentFolder = Paths.get(saveFolder, sid);
        try {
            Files.createDirectories(studentFolder);
        } catch (Exception e) {
            System.out.println(e);
        }

        // Get No. images in folder already
        // e.g. If 0 files, next file will be index 0
        int imageIndex = studentFolder.toFile().list().length;

        // Write to file
        Path output = studentFolder.resolve(String.format("%s_%02d.jpg", sid, imageIndex));
        Imgcodecs.imwrite(output.toString(),
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