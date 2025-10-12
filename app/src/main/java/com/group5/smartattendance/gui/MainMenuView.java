package gui;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import gui.*;

public class MainMenuView extends JFrame {
    // Class for the main menu
    private JLabel headerText;
    private JButton btnQuit;

    private JButton btn00;
    private JButton btn01;
    private JButton btn02;
    private JButton btn03;
    private JButton btn04;
    private JButton btn05;

    public MainMenuView() {
        // Designing UI
        setLayout(null);

        // JFrame Title
        this.setTitle("Student Registration System");

        // Button Element
        final int window_w = 640;
        final int window_h = 400;

        final int btn_w = 240;
        final int btn_h = 40;
        final int btnMargin_t = 80; // top margin
        final int btnMargin_b = 40; // bottom margin
        final int btnMargin_x = 40; // left/right margin
        final int btnGap = 20;

        btnQuit = new JButton("Quit");
        btnQuit.setBounds(
                (window_w - btn_w) / 2,
                window_h - 2 * btn_h - btnMargin_b,
                btn_w, btn_h); // x (horizontal), y (vertical),
                               // width, height
        add(btnQuit);

        btn00 = new JButton("View Students");
        btn00.setBounds(btnMargin_x, btnMargin_t, btn_w, btn_h); // x (horizontal), y (vertical), width, height
        add(btn00);

        btn01 = new JButton("View Sessions");
        btn01.setBounds(window_w - btn_w - btnMargin_x - 14, btnMargin_t, btn_w, btn_h); // idk why theres a 14px gap
        add(btn01);

        btn02 = new JButton("View Reports");
        btn02.setBounds(btnMargin_x, btnMargin_t + (btn_h + btnGap), btn_w, btn_h);
        add(btn02);

        btn03 = new JButton("Settings");
        btn03.setBounds(window_w - btn_w - btnMargin_x - 14, btnMargin_t + (btn_h + btnGap), btn_w, btn_h);
        add(btn03);

        btn04 = new JButton("Register Student");
        btn04.setBounds(btnMargin_x, btnMargin_t + 2 * (btn_h + btnGap), btn_w, btn_h);
        add(btn04);

        btn05 = new JButton("Mark Attendence");
        btn05.setBounds(window_w - btn_w - btnMargin_x - 14, btnMargin_t + 2 * (btn_h + btnGap), btn_w, btn_h);
        add(btn05);

        // Name Element
        headerText = new JLabel("Student Attendence System", SwingConstants.CENTER);
        final int headerMargin_t = 20;
        headerText.setBounds(0, headerMargin_t, window_w, 40);
        // System.out.println(headerText.getFont());
        headerText.setFont(new Font("Dialog", Font.PLAIN, 24));
        add(headerText);

        setSize(new Dimension(window_w, window_h)); // w, h
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        buttonEvents();
    }

    private void buttonEvents() {
        // Calling this method will alow for button actions to be listened to
        // If this method isn't called, butttons will do nothing
        // Therefore this method is currently being called in the constructor

        // Button Event Listener
        btnQuit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Quitting...");
                dispose();
            }
        });

        btn00.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("StudentView");
            }
        });

        btn01.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("SessionView");
            }
        });

        btn02.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("ReportView");
            }
        });

        btn03.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("SettingView");
            }
        });

        btn04.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("LiveCaptureView");
                LiveCaptureView liveCaptureView = new LiveCaptureView(); // Launch LiveCaptureView

                // Start camera in thread
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        liveCaptureView.startCamera();
                    }
                }).start();
            }
        });

        btn05.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("LiveRecognitionView");
                LiveRecognitionView liveRecognitionView = new LiveRecognitionView(); // Launch LiveRecognitionView

                // Start camera in thread
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        liveRecognitionView.startCamera();
                    }
                }).start();
            }
        });
    }

    public static void main(String[] args) {
        new MainMenuView();
    }
}
