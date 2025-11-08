package com.group5.smartattendance.gui;

import com.group5.smartattendance.user.User;
import com.group5.smartattendance.user.UserService;
import com.group5.smartattendance.user.AuthManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginView extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(LoginView.class);

    private JTextField emailField;
    private JTextField fullNameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton signupButton;
    private JButton switchToSignupButton;
    private JButton switchToLoginButton;
    private JLabel errorLabel;
    private boolean isLoginMode = true;

    public LoginView() {
        logger.info("Initializing LoginView");
        initializeUI();
        setupEventHandlers();
    }

    private void initializeUI() {
        setTitle("Smart Attendance System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        setSize(600, 400);
        setResizable(false);
        setLocationRelativeTo(null);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Title
        JLabel titleLabel = new JLabel("Smart Attendance System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0)); // Add top/bottom padding
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        add(titleLabel, gbc);

        // Email
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Email:"), gbc);

        emailField = new JTextField(20);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        add(emailField, gbc);

        // Full Name (initially hidden)
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel fullNameLabel = new JLabel("Full Name:");
        add(fullNameLabel, gbc);

        fullNameField = new JTextField(20);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        add(fullNameField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Password:"), gbc);

        passwordField = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        add(passwordField, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        loginButton = new JButton("Login");
        signupButton = new JButton("Sign Up");

        switchToSignupButton = new JButton("Need an account? Sign up");
        switchToSignupButton.setBorderPainted(false);
        switchToSignupButton.setContentAreaFilled(false);
        switchToSignupButton.setFocusPainted(false);
        switchToSignupButton.setForeground(Color.BLUE);
        switchToSignupButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        switchToLoginButton = new JButton("Already have an account? Login");
        switchToLoginButton.setBorderPainted(false);
        switchToLoginButton.setContentAreaFilled(false);
        switchToLoginButton.setFocusPainted(false);
        switchToLoginButton.setForeground(Color.BLUE);
        switchToLoginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        buttonPanel.add(loginButton);
        buttonPanel.add(signupButton);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(buttonPanel, gbc);

        // Switch mode buttons
        JPanel switchPanel = new JPanel(new FlowLayout());
        switchPanel.add(switchToSignupButton);
        switchPanel.add(switchToLoginButton);

        gbc.gridy = 5;
        add(switchPanel, gbc);

        // Error label
        errorLabel = new JLabel("", SwingConstants.CENTER);
        errorLabel.setForeground(Color.RED);
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        add(errorLabel, gbc);

        updateUIMode();
        // enables button press on "Enter" but looks ugly
        getRootPane().setDefaultButton(loginButton);
        setVisible(true);
    }

    private void setupEventHandlers() {
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLogin();
            }
        });

        signupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleSignup();
            }
        });

        switchToSignupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isLoginMode = false;
                updateUIMode();
                getRootPane().setDefaultButton(signupButton);
            }
        });

        switchToLoginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isLoginMode = true;
                updateUIMode();
                getRootPane().setDefaultButton(loginButton);
            }
        });
    }

    private void updateUIMode() {
        // Find the full name components in the component hierarchy
        Component[] components = getContentPane().getComponents();
        for (Component comp : components) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                // set full name visible if not in login mode (aka in sign up mode)
                if ("Full Name:".equals(label.getText())) {
                    comp.setVisible(!isLoginMode);
                }
            } else if (comp instanceof JTextField) {
                JTextField field = (JTextField) comp;
                if (field == fullNameField) {
                    field.setVisible(!isLoginMode);
                    // clear full name field when switching to login mode
                    if (isLoginMode) {
                        field.setText("");
                    }
                }
            } else if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                Component[] panelComponents = panel.getComponents();
                for (Component panelComp : panelComponents) {
                    if (panelComp == loginButton) {
                        // show login button if in login mode
                        panelComp.setVisible(isLoginMode);
                    } else if (panelComp == signupButton) {
                        // show signup button if not in login mode
                        panelComp.setVisible(!isLoginMode);
                    } else if (panelComp == switchToSignupButton) {
                        // show switch to signup button if in login mode
                        panelComp.setVisible(isLoginMode);
                    } else if (panelComp == switchToLoginButton) {
                        // show switch to login button if not in login mode
                        panelComp.setVisible(!isLoginMode);
                    }
                }
            }
        }

        // Update window title
        setTitle(isLoginMode ? "Smart Attendance System - Login" : "Smart Attendance System - Sign Up");

        // Clear any error messages when switching modes
        errorLabel.setText("");
    }

    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        try {
            User user = UserService.login(email, password);
            logger.info("Login successful for user: {}", email);
            // clear fields
            errorLabel.setText("");
            emailField.setText("");
            passwordField.setText("");

            // store session user
            AuthManager.setCurrentUser(user);

            // Close login window and open main menu
            dispose();
            new MainMenuView();

        } catch (Exception ex) {
            logger.warn("Login failed: {}", ex.getMessage());
            errorLabel.setForeground(Color.RED);
            errorLabel.setText(ex.getMessage());
        }
    }

    private void handleSignup() {
        String email = emailField.getText().trim();
        String fullName = fullNameField.getText().trim();
        String password = new String(passwordField.getPassword());

        try {
            User user = UserService.signup(email, fullName, password);
            logger.info("Signup successful for user: {}", user.getEmail());
            errorLabel.setForeground(new Color(34, 139, 34));
            errorLabel.setText("Account created successfully! You can now log in.");
            emailField.setText("");
            fullNameField.setText("");
            passwordField.setText("");

        } catch (Exception ex) {
            logger.warn("Signup failed: {}", ex.getMessage());
            errorLabel.setForeground(Color.RED);
            errorLabel.setText(ex.getMessage());
        }
    }
}
