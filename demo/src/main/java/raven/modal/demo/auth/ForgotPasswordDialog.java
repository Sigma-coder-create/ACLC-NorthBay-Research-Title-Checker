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
import java.util.Random;

public class ForgotPasswordDialog extends JDialog {

    private JPanel cardPanel;
    private CardLayout cardLayout;

    // Page 1 – email
    private JTextField txtEmail;

    // Page 2 – code + new password
    private JTextField txtCode;
    private JPasswordField txtNewPassword;
    private JPasswordField txtConfirmPassword;

    private String generatedCode;
    private String targetEmail;

    public ForgotPasswordDialog(Frame owner) {
        super(owner, "Reset Password", true);
        init();
    }

    private void init() {
        setLayout(new MigLayout("fill,insets 20", "[fill,450]"));
        setResizable(false);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // ---- Card 1: enter email ----
        JPanel page1 = new JPanel(new MigLayout("fillx,wrap,insets 0", "[fill,300]"));
        page1.putClientProperty(FlatClientProperties.STYLE, "background:null;");

        JLabel lbTitle = new JLabel("Reset your password");
        lbTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +12;");
        page1.add(lbTitle, "gapbottom 15");

        page1.add(new JLabel("Enter your registered email"), "gapbottom 2");
        txtEmail = new JTextField();
        txtEmail.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "you@example.com");
        txtEmail.putClientProperty(FlatClientProperties.STYLE, "margin:4,10,4,10;arc:12;");
        page1.add(txtEmail, "wrap, gapbottom 15");

        JButton btnSendCode = new JButton("Send Verification Code");
        btnSendCode.putClientProperty(FlatClientProperties.STYLE, "arc:12;");
        btnSendCode.addActionListener(e -> sendCode());
        page1.add(btnSendCode, "width 200, right");

        cardPanel.add(page1, "email");

        // ---- Card 2: code + new password ----
        JPanel page2 = new JPanel(new MigLayout("fillx,wrap,insets 0","[fill,300]"));
        page2.putClientProperty(FlatClientProperties.STYLE, "background:null;"); 

        page2.add(new JLabel("Check your email for the code"), "gapbottom 15");

        page2.add(new JLabel("Verification Code"), "gapbottom 2");
        txtCode = new JTextField();
        txtCode.putClientProperty(FlatClientProperties.STYLE, "margin:4,10,4,10;arc:12;");
        page2.add(txtCode, "wrap, gapbottom 10");

        page2.add(new JLabel("New Password"), "gapbottom 2");
        txtNewPassword = new JPasswordField();
        txtNewPassword.putClientProperty(FlatClientProperties.STYLE, "margin:4,10,4,10;arc:12;showRevealButton:true;");
        page2.add(txtNewPassword, "wrap, gapbottom 10");

        page2.add(new JLabel("Confirm New Password"), "gapbottom 2");
        txtConfirmPassword = new JPasswordField();
        txtConfirmPassword.putClientProperty(FlatClientProperties.STYLE, "margin:4,10,4,10;arc:12;showRevealButton:true;");
        page2.add(txtConfirmPassword, "wrap, gapbottom 15");

        JButton btnReset = new JButton("Reset Password");
        btnReset.putClientProperty(FlatClientProperties.STYLE, "arc:12;");
        btnReset.addActionListener(e -> doReset());
        page2.add(btnReset, "split 2, width 150");

        JButton btnBack = new JButton("Back");
        btnBack.putClientProperty(FlatClientProperties.STYLE, "arc:12;");
        btnBack.addActionListener(e -> cardLayout.show(cardPanel, "email"));
        page2.add(btnBack, "width 100");

        cardPanel.add(page2, "code");

        add(cardPanel, "grow");
        pack();
        setMinimumSize(new Dimension(450, getPreferredSize().height));
        setLocationRelativeTo(getOwner());
    }

    private void sendCode() {
        targetEmail = txtEmail.getText().trim();
        if (targetEmail.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your email.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Verify email exists
        try (Connection conn = DBConnection.getMySQLConnection()) {
            if (conn == null) {
                JOptionPane.showMessageDialog(this, "Database not available.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE email = ?");
            ps.setString(1, targetEmail);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "No account found with that email.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            rs.close();
            ps.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Generate code & send email
        generatedCode = String.format("%06d", new Random().nextInt(999999));
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                EmailUtil.sendVerificationCode(targetEmail, generatedCode);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(ForgotPasswordDialog.this, 
                            "A verification code has been sent to " + targetEmail);
                    cardLayout.show(cardPanel, "code");
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(ForgotPasswordDialog.this, 
                            "Failed to send email: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void doReset() {
        String code = txtCode.getText().trim();
        String newPass = new String(txtNewPassword.getPassword());
        String confirm = new String(txtConfirmPassword.getPassword());

        if (code.isEmpty() || newPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!newPass.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (newPass.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!code.equals(generatedCode)) {
            JOptionPane.showMessageDialog(this, "Invalid verification code.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DBConnection.getMySQLConnection()) {
            if (conn == null) {
                JOptionPane.showMessageDialog(this, "Database not available.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String hashed = BCrypt.withDefaults().hashToString(12, newPass.toCharArray());
            PreparedStatement ps = conn.prepareStatement("UPDATE users SET password_hash = ? WHERE email = ?");
            ps.setString(1, hashed);
            ps.setString(2, targetEmail);
            ps.executeUpdate();
            ps.close();

            JOptionPane.showMessageDialog(this, "Password has been reset. You can now log in.");
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Reset failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}