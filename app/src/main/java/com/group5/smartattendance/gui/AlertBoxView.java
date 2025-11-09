package com.group5.smartattendance.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class AlertBoxView extends JFrame {
    // open new alertBoxView window
    // Student has been registered with SID: ...
    // close alertBoxView and currentView
    // Button Element
    final int window_w = 720;
    final int window_h = 240;

    private JLabel headerText;

    private JButton btnOk;

    public AlertBoxView(String message, String titleMsg) {
        // Designing UI
        setLayout(null);

        // JFrame Title
        this.setTitle(titleMsg);

        // UI Elements
        btnOk = new JButton("Close");
        btnOk.setBounds((window_w - 240) / 2, window_h - 120, 240, 40);
        add(btnOk);

        // Header Text
        headerText = new JLabel(message, SwingConstants.CENTER);
        final int headerMargin_t = 20;
        headerText.setBounds(0, headerMargin_t, window_w, 40);
        headerText.setFont(new Font("Dialog", Font.PLAIN, 24));
        add(headerText);

        setSize(new Dimension(window_w, window_h)); // w, h
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        buttonEvents();
    }

    private void buttonEvents() {
        btnOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }
}
