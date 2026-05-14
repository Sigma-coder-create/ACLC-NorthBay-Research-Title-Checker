package raven.modal.demo.auth;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.component.LabelButton;
import raven.modal.demo.menu.MyDrawerBuilder;
import raven.modal.demo.model.ModelUser;
import raven.modal.demo.system.Form;
import raven.modal.demo.system.FormManager;

import com.finals.db.DBConnection;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import at.favre.lib.crypto.bcrypt.BCrypt;

public class Login extends Form {

    public Login() {
        init();
    }

    private void init() {
        setLayout(new MigLayout("al center center"));
        createLogin();
    }

    private void createLogin() {
        JPanel panelLogin = new JPanel(new MigLayout());

        JPanel loginContent = new JPanel(new MigLayout("fillx,wrap,insets 35 35 25 35", "[fill,300]"));

        JLabel lbTitle = new JLabel("Welcome back!");
        JLabel lbDescription = new JLabel("Please sign in to access your account");
        lbTitle.putClientProperty(FlatClientProperties.STYLE, "" +
                "font:bold +12;");

        loginContent.add(lbTitle);
        loginContent.add(lbDescription);

        JTextField txtUsername = new JTextField();
        JPasswordField txtPassword = new JPasswordField();
        JCheckBox chRememberMe = new JCheckBox("Remember Me");
        JButton cmdLogin = new JButton("Login") {
            @Override
            public boolean isDefaultButton() {
                return true;
            }
        };

        // style
        txtUsername.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter your username or email");
        txtPassword.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter your password");

        panelLogin.putClientProperty(FlatClientProperties.STYLE, "" +
                "[light]border:5,5,5,5,shade($Panel.background,10%),,20;" +
                "[dark]border:5,5,5,5,tint($Panel.background,5%),,20;" +
                "[light]background:shade($Panel.background,3%);" +
                "[dark]background:tint($Panel.background,2%);");

        loginContent.putClientProperty(FlatClientProperties.STYLE, "" +
                "background:null;");

        txtUsername.putClientProperty(FlatClientProperties.STYLE, "" +
                "margin:4,10,4,10;" +
                "arc:12;");
        txtPassword.putClientProperty(FlatClientProperties.STYLE, "" +
                "margin:4,10,4,10;" +
                "arc:12;" +
                "showRevealButton:true;");

        cmdLogin.putClientProperty(FlatClientProperties.STYLE, "" +
                "margin:4,10,4,10;" +
                "arc:12;");

        loginContent.add(new JLabel("Username"), "gapy 25");
        loginContent.add(txtUsername);

        loginContent.add(new JLabel("Password"), "gapy 10");
        loginContent.add(txtPassword);
        loginContent.add(chRememberMe, "grow 0");
        loginContent.add(cmdLogin, "gapy 20");
        //loginContent.add(createInfo());

        panelLogin.add(loginContent);
        add(panelLogin);

        // event
        cmdLogin.addActionListener(e -> {
            String userName = txtUsername.getText();
            String password = String.valueOf(txtPassword.getPassword());

            ModelUser user = getUser(userName, password);

            if (user != null) {
                MyDrawerBuilder.getInstance().setUser(user);
                FormManager.login();
            } else {
                JOptionPane.showMessageDialog(null, "Invalid email or password");
            }
        });
    }

    /*private JPanel createInfo() {
        JPanel panelInfo = new JPanel(new MigLayout("wrap,al center", "[center]"));
        panelInfo.putClientProperty(FlatClientProperties.STYLE, "" +
                "background:null;");

        // "Create one here" label
       // JLabel lbNoAccount = new JLabel("Don't have an account?");
        //JLabel lbCreate = new JLabel("<html><u>Create one here</u></html>");
        lbCreate.setForeground(new Color(0, 102, 204));
        lbCreate.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lbCreate.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Window window = SwingUtilities.getWindowAncestor(panelInfo);
                RegisterDialog registerDlg = new RegisterDialog((Frame) window);
                registerDlg.setVisible(true);
            }
        });
        panelInfo.add(lbNoAccount, "split 2");
        panelInfo.add(lbCreate);

        // "Forgot password?" label
        JLabel lbForgot = new JLabel("<html><u>Forgot your password?</u></html>");
        lbForgot.setForeground(new Color(0, 102, 204));
        lbForgot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lbForgot.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Window window = SwingUtilities.getWindowAncestor(panelInfo);
                ForgotPasswordDialog forgot = new ForgotPasswordDialog((Frame) window);
                forgot.setVisible(true);
            }
        });
        panelInfo.add(lbForgot, "gapy 10");

        // Contact info (optional – you can keep or remove)
        panelInfo.add(new JLabel("Contact us at"), "split 2, gapy 10");
        LabelButton lbLink = new LabelButton("help@info.com");
        panelInfo.add(lbLink);

        return panelInfo;
    }*/

    private ModelUser getUser(String emailOrUsername, String password) {
        ModelUser user = null;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getMySQLConnection();
            if (conn == null) {
                JOptionPane.showMessageDialog(null, "MySQL server is not available.");
                return null;
            }

            String sql = "SELECT username, email, password_hash, role FROM users WHERE username = ? OR email = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, emailOrUsername);
            ps.setString(2, emailOrUsername);
            rs = ps.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                String username = rs.getString("username");
                String email = rs.getString("email");
                String roleStr = rs.getString("role");

                BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), storedHash);
                if (result.verified) {
                    ModelUser.Role role = "teacher".equalsIgnoreCase(roleStr)
                            ? ModelUser.Role.TEACHER : ModelUser.Role.STUDENT;

                    // Allow only teachers
                    if (role == ModelUser.Role.TEACHER) {
                        user = new ModelUser(username, email, role);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
        }
        return user;
    }
}