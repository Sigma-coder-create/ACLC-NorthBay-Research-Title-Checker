package raven.modal.demo.auth;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import com.finals.db.DBConnection;
import at.favre.lib.crypto.bcrypt.BCrypt;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RegisterDialog extends JDialog {

    private JTextField txtUsername;
    private JTextField txtEmail;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmPassword;

    public RegisterDialog(Frame owner) {
        super(owner, "Create New Account", true);
        init();
    }

    private void init() {
        setLayout(new MigLayout("fill,insets 20", "[fill,300]"));
        setResizable(false);

        JLabel lbTitle = new JLabel("Create your account");
        lbTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +12;");
        add(lbTitle, "wrap, gapbottom 15");

        add(new JLabel("Username"), "gapbottom 2");
        txtUsername = new JTextField();
        txtUsername.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Choose a username");
        txtUsername.putClientProperty(FlatClientProperties.STYLE, "margin:4,10,4,10;arc:12;");
        add(txtUsername, "wrap, gapbottom 10");

        add(new JLabel("Email"), "gapbottom 2");
        txtEmail = new JTextField();
        txtEmail.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "you@example.com");
        txtEmail.putClientProperty(FlatClientProperties.STYLE, "margin:4,10,4,10;arc:12;");
        add(txtEmail, "wrap, gapbottom 10");

        add(new JLabel("Password"), "gapbottom 2");
        txtPassword = new JPasswordField();
        txtPassword.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "At least 6 characters");
        txtPassword.putClientProperty(FlatClientProperties.STYLE, "margin:4,10,4,10;arc:12;showRevealButton:true;");
        add(txtPassword, "wrap, gapbottom 10");

        add(new JLabel("Confirm Password"), "gapbottom 2");
        txtConfirmPassword = new JPasswordField();
        txtConfirmPassword.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Re-enter your password");
        txtConfirmPassword.putClientProperty(FlatClientProperties.STYLE, "margin:4,10,4,10;arc:12;showRevealButton:true;");
        add(txtConfirmPassword, "wrap, gapbottom 10");

        JButton btnRegister = new JButton("Register");
        btnRegister.putClientProperty(FlatClientProperties.STYLE, "arc:12;");
        btnRegister.addActionListener(e -> doRegister());

        JButton btnCancel = new JButton("Cancel");
        btnCancel.putClientProperty(FlatClientProperties.STYLE, "arc:12;");
        btnCancel.addActionListener(e -> dispose());

        add(btnRegister, "split 2, right, width 120");
        add(btnCancel, "width 120");

        pack();
        setLocationRelativeTo(getOwner());
    }

    private void doRegister() {
        String username = txtUsername.getText().trim();
        String email = txtEmail.getText().trim();
        String password = new String(txtPassword.getPassword());
        String confirm = new String(txtConfirmPassword.getPassword());

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!password.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (password.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DBConnection.getMySQLConnection()) {
            if (conn == null) {
                JOptionPane.showMessageDialog(this, "Database not available.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check if username or email already taken
            PreparedStatement checkPs = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ? OR email = ?");
            checkPs.setString(1, username);
            checkPs.setString(2, email);
            ResultSet rs = checkPs.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this, "Username or email already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            rs.close();
            checkPs.close();

            // Hash the password
            String hashed = BCrypt.withDefaults().hashToString(12, password.toCharArray());

            // Insert with role = 'student' (hardcoded)
            PreparedStatement insertPs = conn.prepareStatement(
                "INSERT INTO users (username, email, password_hash, role) VALUES (?, ?, ?, 'student')"
            );
            insertPs.setString(1, username);
            insertPs.setString(2, email);
            insertPs.setString(3, hashed);
            insertPs.executeUpdate();
            insertPs.close();

            JOptionPane.showMessageDialog(this, "Account created successfully! You can now log in.");
            dispose();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Registration failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}