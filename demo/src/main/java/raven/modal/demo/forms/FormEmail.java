package raven.modal.demo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.SystemForm;
import raven.modal.demo.auth.EmailUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

@SystemForm(name = "Email", description = "Send an email")
public class FormEmail extends Form {

    private JTextField txtTo, txtSubject;
    private JTextArea txtBody;

    public FormEmail() {
        init();
    }

    private void init() {
        setLayout(new MigLayout("fill,wrap,insets 20", "[fill,grow]", "[][][][grow][]"));

        JLabel title = new JLabel("Compose Email");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +3;");
        add(title, "span 2");

        add(new JLabel("To:"));
        txtTo = new JTextField();
        txtTo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "recipient@example.com");
        add(txtTo, "growx");

        add(new JLabel("Subject:"));
        txtSubject = new JTextField();
        add(txtSubject, "growx");

        add(new JLabel("Message:"));
        txtBody = new JTextArea(8, 40);
        txtBody.setLineWrap(true);
        txtBody.setWrapStyleWord(true);
        JScrollPane scrollBody = new JScrollPane(txtBody);
        add(scrollBody, "grow, pushy");

        JButton btnSend = new JButton("Send");
        btnSend.addActionListener(this::sendEmail);
        add(btnSend, "split 2, right, width 100");

        JButton btnClear = new JButton("Clear");
        btnClear.addActionListener(e -> {
            txtTo.setText("");
            txtSubject.setText("");
            txtBody.setText("");
        });
        add(btnClear, "width 100");
    }

    private void sendEmail(ActionEvent e) {
        String to = txtTo.getText().trim();
        String subject = txtSubject.getText().trim();
        String body = txtBody.getText().trim();

        if (to.isEmpty() || subject.isEmpty() || body.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Send in background to avoid UI freeze
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // EmailUtil.sendVerificationCode sends a code; we need a generic send method.
                // So we'll directly use the same SMTP logic but with a custom subject/body.
                // We'll reuse the send method from EmailUtil by adding a new overload.
                EmailUtil.sendEmail(to, subject, body);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(FormEmail.this, "Email sent successfully!");
                    txtTo.setText("");
                    txtSubject.setText("");
                    txtBody.setText("");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(FormEmail.this, "Failed to send: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}