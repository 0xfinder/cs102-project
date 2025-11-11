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

// Importing date class of sql package
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import com.group5.smartattendance.student.FaceData;
import com.group5.smartattendance.student.Student;
import com.group5.smartattendance.core.CascadeLoader;
import com.group5.smartattendance.marker.AutoMarker;
import com.group5.smartattendance.marker.MarkingRequest;
import com.group5.smartattendance.marker.AttendanceRecord.Status;
import com.group5.smartattendance.marker.AttendanceManager;
import com.group5.smartattendance.marker.AttendanceMarker;
import com.group5.smartattendance.marker.AttendanceRecord;
import com.group5.smartattendance.session.Roster;
import com.group5.smartattendance.session.Session;
import com.group5.smartattendance.session.SessionManager;
import com.group5.smartattendance.persistence.StudentManager;
import com.group5.smartattendance.student.HistogramData;
import com.group5.smartattendance.core.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Class - Swing Class
public class LiveRecognitionView extends JFrame {
    // Swing Elements
    private JLabel cameraScreen;
    private JButton btnMarkAttendance;
    private JLabel nameLabel;
    private JLabel confidenceLabel;
    private JButton btnBack;
    private JLabel statusLabel;
    private JLabel logLabel;
    private JComboBox dropdownSession;

    // Start camera
    private VideoCapture capture;
    private boolean cameraIsRunning = true;

    // Store image as 2D matrix
    private Mat webcamFrame;
    private Mat gray;

    private String detectedStudent = "No Student Detected"; // or "Unknown Student"

    // OpenCV Stuff
    // private String saveFolder = "images";
    private static CascadeClassifier faceDetector = CascadeLoader.loadDefaultFaceCascade();
    private double detectedStudentScore = 0.0;
    private String detectedStudentID;
    // private boolean currFaceDetected = false; // if a face is detected in the
    // current frame

    // Bounding Box Variables
    // NOTE: Java Colors are in BGR
    final Scalar bbColorGreen = new Scalar(0, 255, 0);
    final Scalar bbColorSuccess = new Scalar(69, 167, 40);
    final Scalar bbColorWarning = new Scalar(7, 193, 255);
    final Scalar bbColorDanger = new Scalar(69, 53, 220);
    final Scalar bbColorWhite = new Scalar(255, 255, 255);
    final double bbTextSize = 0.75; // default from demo: 0.9
    final int bbTextThickness = 2; // default from demo: 2

    // Misc Variables
    private Configuration config = Configuration.getInstance();
    final double threshold = config.getRecognitionThreshold();
    final int webcamIndex = config.getCameraIndex();

    private SessionManager sm = new SessionManager();
    // private Session currSession;
    private String selectedSessionID;
    private List<FaceData> studentFaceData = new ArrayList<>();

    private long lastCaptureTime = System.currentTimeMillis(); // in milliseconds
    final int prepareTimeSeconds = 1;
    final long cooldownTime = config.getCooldownSeconds() * 1000; // in milliseconds
    private boolean sessionStarted = false;

    // Logging
    private static final Logger logger = LoggerFactory.getLogger(LiveRecognitionView.class);

    public LiveRecognitionView() {
        logger.info("Initialising LiveRecognitionView");
        // Designing UI
        setLayout(null);

        // JFrame Title
        this.setTitle("Student Registration System");

        // Webcam Screen
        cameraScreen = new JLabel();
        cameraScreen.setBounds(0, 0, 640, 480); // x (horizontal), y (vertical), width, height
        add(cameraScreen);

        // Button Element
        btnMarkAttendance = new JButton("Start Automarker");
        btnMarkAttendance.setBounds(320 - 240, 480, 240, 40);
        add(btnMarkAttendance);

        btnBack = new JButton("Back");
        btnBack.setBounds(0, 560, 80, 40);
        add(btnBack);

        // Get Sessions
        logger.info("Fetching session data");
        List<Session> sessions = sm.listSessions();
        List<String> sessionNames = new ArrayList<>();
        for (Session session : sessions) {
            if (session.getStatus() == Session.Status.OPEN)
                sessionNames.add("C" + session.getId() + " - " + session.getCourseName());
        }

        // Dropdown
        String[] sessionNamesArr = { "No Sessions Found" };

        if (sessionNames.size() != 0) {
            sessionNamesArr = sessionNames.toArray(new String[0]);
        }

        dropdownSession = new JComboBox(sessionNamesArr);
        dropdownSession.setBounds(640 - 280, 480, 240, 40);
        dropdownSession.setSelectedIndex(0);
        add(dropdownSession);

        // Label Elements
        nameLabel = new JLabel("Detected Student: " + detectedStudent);
        nameLabel.setBounds(640 - 280, 520, 240, 40);
        add(nameLabel);

        confidenceLabel = new JLabel("Confidence: -");
        confidenceLabel.setBounds(640 - 280, 560, 240, 40);
        add(confidenceLabel);

        statusLabel = new JLabel("Marker status: Idle");
        statusLabel.setBounds(320 - 240, 520, 240, 40);
        add(statusLabel);

        logLabel = new JLabel("");
        logLabel.setBounds(320 - 240, 560, 280, 40);
        add(logLabel);

        // Button Event Listener
        buttonEvents();

        setSize(new Dimension(640, 640));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void buttonEvents() {
        btnMarkAttendance.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedSessionID != null) {
                    if (sessionStarted == false) {
                        logger.info("Starting Attendance Marker");
                        // sm.openSession(selectedSessionID);
                        sessionStarted = true;

                        // Update UI
                        statusLabel.setText("Marker status: Running");
                        btnMarkAttendance.setText("Stop Automarker");

                        // Init capture Time
                        lastCaptureTime = System.currentTimeMillis();
                    } else {
                        String[] closeSessionButtons = { "Stop", "Don't Stop" };
                        int returnVal = confirmBox(closeSessionButtons,
                                "You are about to stop attendance taking for the session. Continue?",
                                "Confirm Stop Attendance Taking");
                        // 0 - Close, 1 - Don't Close
                        if (returnVal == 0) {
                            logger.info("Stopping Attendance Marker");
                            sessionStarted = false;

                            // Update UI
                            statusLabel.setText("Marker status: Idle");
                            btnMarkAttendance.setText("Start Automarker");

                            // Clear capture time
                            lastCaptureTime = 0;
                        }
                    }
                } else {
                    logger.error("Failed to start marker: No session selected");
                    warningBox("Please select a session");
                }
            }
        });

        btnBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logger.info("Preparing to close LiveRecognitionView");
                if (sessionStarted) {
                    System.out.println("Stopping attendance taking");
                }
                sessionStarted = false; // Stop attendance taking loop if still active
                logger.info("Stopping camera");
                cameraIsRunning = false;
                logger.info("Closing LiveRecognitionView");
                dispose(); // dispose method (of JFrame) kills the instance
            }
        });

        dropdownSession.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedSessionID = ((String) dropdownSession.getSelectedItem()).split(" - ")[0].substring(1);
                logger.info(String.format("Selected session ID %s", selectedSessionID));
                logger.info(String.format("Fetching student face data from session ID %s", selectedSessionID));
                studentFaceData = getFaceData(); // get face data from session id
                if (studentFaceData.size() == 0) {
                    logger.warn(String.format("No face data found for session ID %s", selectedSessionID));
                    warningBox(
                            "No student face data found. Please ensure session roster is not empty, and that students have captured face data.");
                }
                // System.out.println(selectedSessionID);
            }
        });
    }

    // Creating a camera
    public void startCamera() {
        // Start Webcam
        logger.info("Starting camera");
        capture = new VideoCapture(webcamIndex);
        if (!capture.isOpened()) {
            // System.out.println("Error opening webcam!");
            logger.error(String.format("Error opening webcam index %d", webcamIndex));
            warningBox("Error opening webcam!");
            return;
        }
        webcamFrame = new Mat();
        byte[] imageData;

        ImageIcon icon;
        while (cameraIsRunning) { // Program loop
            // read image to matrix
            // capture.read(frame);
            if (!capture.read(webcamFrame)) {
                System.out.println("No frame captured!");
                break;
            }

            // Detect face and update frame buffer
            detectFace();

            // Mark Attendance Logic
            if (System.currentTimeMillis() >= lastCaptureTime + cooldownTime && sessionStarted) {
                // Get student record status
                // If confidence is below threshold
                // BUT student is already marked with a higher confidence
                // ignore
                AttendanceRecord studentRecord = null;
                Status studentStatus = null;
                try {
                    studentRecord = AttendanceManager.findBySessionAndStudentId(selectedSessionID, detectedStudentID)
                            .get();
                    studentStatus = studentRecord.getStatus();
                } catch (Exception e) {
                    logger.error("Error: " + e);
                }

                // Actual logic flow
                if (detectedStudent.equals("No Student Detected") || detectedStudent.equals("Unknown Student")) {
                    logger.warn("No student detected");
                    logLabel.setText(detectedStudent);

                } else if (detectedStudentScore >= threshold) { // If confidence is above threshold
                    // Mark Attendance normally;
                    markAttendance();
                } else if (studentStatus == Status.PRESENT || studentStatus == Status.LATE) { // If student is already
                                                                                              // present, ignore
                    // Ignore
                    logger.warn(String.format("Face detected with low confidence: %s, confidence %.5f", detectedStudent,
                            detectedStudentScore));
                    logger.info("Ignoring detection: Student already marked as PRESENT or LATE previously");
                } else {
                    // Pause code and ask for confirmation box
                    logger.warn(String.format("Face detected with low confidence: %s, confidence %.5f", detectedStudent,
                            detectedStudentScore));
                    String[] closeSessionButtons = { "Mark", "Don't Mark" };
                    int returnVal = confirmBox(closeSessionButtons,
                            String.format("Mark Attendence for %s? (Confidence: %.1f", detectedStudent,
                                    detectedStudentScore * 100) + "%)",
                            "Low Confidence Confirmation");
                    // 0 - Close, 1 - Don't Close
                    if (returnVal == 0) {
                        logger.info("Confirming detection");
                        markAttendance(); // Mark Attendance normally;
                    } else {
                        logger.info("Ignoring detection: User declined");
                    }
                }
                lastCaptureTime = System.currentTimeMillis();
            } else {
                // Do nothing
            }

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

    private void detectFace() {
        // from FaceRecognitionDemo.java
        // DO NOT LOG! RUNS EVERY FRAME!
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
        detectedStudent = "No Student Detected";
        String LabelScore = "--.-";
        long sysTime = System.currentTimeMillis();
        // currFaceDetected = false;
        for (Rect rect : faces.toArray()) {
            // Update currFaceDeteced
            // currFaceDetected = true;

            // Crop and resize face
            Mat face = gray.submat(rect);
            Imgproc.resize(face, face, new Size(200, 200));
            Mat faceHist = computeHistogram(face);

            // Compare with training histograms and get data
            HistogramData hd = computeBestChoice(faceHist, studentFaceData);
            detectedStudentID = hd.getHighestScoreID();
            detectedStudentScore = hd.getHighestScore();

            // Update current score
            LabelScore = String.format("%.1f", detectedStudentScore * 100);

            // Prepare data for label
            String formatScore = String.format("%.1f", detectedStudentScore * 100);
            String studentName = "";
            String bbLabelText = "";

            try {
                if (!detectedStudentID.equals("unknown")) { // if face belongs to a student
                    studentName = StudentManager.findById(detectedStudentID).get().getName();
                    detectedStudent = "SID: S" + detectedStudentID + ", " + studentName;
                    bbLabelText = "SID: S" + detectedStudentID + ", " + studentName + " [" + formatScore + "%]";
                } else {
                    detectedStudent = "Unknown Student"; // To be displayed in the Label UI
                    bbLabelText = "Unknown Student"; // To be displayed above the BB
                }
            } catch (Exception e) {
                // System.out.println("LiveREcognitionView.detectFace() " + e);
                logger.error("Error: " + e);
            }

            // Set bounding box color based on best score (correlation: higher is better)
            Scalar bbColor = bbColorGreen; // Bounding Box color defaults to green when within threshold;
            long prepareTime = cooldownTime - (prepareTimeSeconds * 1000); // Time before prepared signal starts
            if (sessionStarted && lastCaptureTime + prepareTime <= sysTime
                    && sysTime <= lastCaptureTime + cooldownTime) {
                bbColor = bbColorWhite; // BB color to white on prepare cooldown
            } else if (detectedStudentScore < threshold) {
                bbColor = bbColorWarning; // BB color to warning when below threshold
            }

            // Draw rectangle
            Imgproc.rectangle(webcamFrame, new Point(rect.x, rect.y),
                    new Point(rect.x + rect.width, rect.y + rect.height),
                    bbColor, 2);

            // Draw Text
            Imgproc.putText(webcamFrame, bbLabelText, new Point(rect.x, rect.y - 10),
                    Imgproc.FONT_HERSHEY_SIMPLEX, bbTextSize, bbColor, bbTextThickness); // Font Family, ???, RGB Color,
                                                                                         // ???

            // Only take the first face guess of array
            break;
        }

        // Update (other) label text
        nameLabel.setText("Detected Student: " + detectedStudent);
        confidenceLabel.setText("Confidence: " + LabelScore + "%");

        // Display frame
        BufferedImage image = matToBufferedImage(webcamFrame);
        label.setIcon(new ImageIcon(image));
        label.repaint();
    }

    // Compute histogram for a single image
    private static Mat computeHistogram(Mat image) {
        // from faceRecognitionDemo.java
        Mat hist = new Mat();
        MatOfInt histSize = new MatOfInt(256);
        MatOfFloat ranges = new MatOfFloat(0f, 256f);
        MatOfInt channels = new MatOfInt(0);

        Imgproc.calcHist(List.of(image), channels, new Mat(), hist, histSize, ranges);
        // hist.convertTo(hist, 0); // Convert from CV_32FC1 to CV_8UC1
        // Imgproc.equalizeHist(hist, hist);
        // hist.convertTo(hist, 5); // Convert from CV_8UC1 to CV_32FC1
        Core.normalize(hist, hist, 0, 1, Core.NORM_MINMAX);
        return hist;
    }

    // Convert Mat to BufferedImage for display
    private static BufferedImage matToBufferedImage(Mat mat) {
        // from faceRecognitionDemo.java
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

    // To update with only students from a given session roster, not all students
    private List<FaceData> getFaceData() {
        List<FaceData> studentFaceData = new ArrayList<>();

        // OLD METHOD: Read from folder name
        // File folder = new File(saveFolder);
        // if (!folder.exists() || !folder.isDirectory()) {
        // return studentFaceData;
        // }

        // String[] studentsNames = folder.list();
        // if (studentsNames != null) {
        // for (String name : studentsNames) {
        // if (!name.equals("unnamed")) {
        // studentFaceData.add(createFaceData(name));
        // }
        // }
        // }

        try {
            if (selectedSessionID != null) {
                Roster roster = sm.loadRoster(selectedSessionID);
                List<Student> studentList = roster.getStudents();
                // List<Student> studentList = StudentManager.findAll();
                for (Student student : studentList) {
                    studentFaceData.add(student.getFaceData());
                }
            }
        } catch (Exception e) {
            // System.out.println("LiveRecognitionView.getFaceData() " + e);
            logger.error("Error: " + e);
        }
        return studentFaceData;

    }

    private HistogramData computeBestChoice(Mat faceHist, List<FaceData> studentFaceData) {
        // double highestScore = 0.0;
        // String highestScoreID = "unknown";
        HistogramData hd = new HistogramData();

        for (FaceData studentFD : studentFaceData) {
            double currScore = studentFD.getBestHistogramScore(faceHist);
            // debug
            // System.out.println(studentFD.getStudentName() + " " + currScore);
            // if (currScore > threshold && currScore > hd.getHighestScore()) { // threshold
            // data
            if (currScore > hd.getHighestScore()) { // threshold data
                hd.setHighestScore(currScore);
                hd.setHighestScoreID(studentFD.getStudentID());
            }
        }

        return hd;
    }

    private void markAttendance() {
        String labelMessage = "";
        Instant markedAt = Instant.now();

        if (detectedStudent.equals("No Student Detected")) {
            labelMessage = "No Face Detected!";
            logger.error(String.format("Failed to mark attendance: No face detected"));
        } else {
            try {
                // Get MarkingRequest instance
                MarkingRequest request = MarkingRequest.builder(selectedSessionID, detectedStudentID)
                        .markedAt(markedAt)
                        .confidence(detectedStudentScore)
                        .notes("").build();

                AttendanceMarker marker = new AutoMarker();
                AttendanceRecord newRecord = marker.markAttendance(request);

                labelMessage = String.format("Attendence Marked for %s", detectedStudent);
                // Logging done in AttendanceManaer

            } catch (Exception e) {
                // System.out.println("LiveRecognitionView.markAttendance() " + e);
                logger.error("Error: " + e);
            }

        }

        logLabel.setText(labelMessage);
        System.out.println(labelMessage);
    }

    private void warningBox(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }

    private int confirmBox(String[] buttons, String msg, String title) {
        int returnVal = JOptionPane.showOptionDialog(
                this, msg, title, JOptionPane.WARNING_MESSAGE, 0,
                null, buttons, buttons[0]);
        return returnVal;
    }

    // UNUSED: Main driver method
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