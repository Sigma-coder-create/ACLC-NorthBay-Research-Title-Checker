package raven.modal.demo.auth;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;
import java.io.InputStream;
import java.io.ByteArrayInputStream;          // required for decryption
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class EmailUtil {

    private static final String GMAIL_USERNAME;
    private static final String APP_PASSWORD;

    static {
        Properties props = new Properties();
        boolean loaded = false;

        // 1. Try to load the encrypted file
        try (InputStream encIn = EmailUtil.class.getResourceAsStream(
                "/raven/modal/demo/data/gmail.properties.enc")) {
            if (encIn != null) {
                byte[] encBytes = encIn.readAllBytes();
                SecretKeySpec keySpec = new SecretKeySpec("16ByteSecretKey!".getBytes("UTF-8"), "AES");
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.DECRYPT_MODE, keySpec);
                byte[] decrypted = cipher.doFinal(encBytes);
                props.load(new ByteArrayInputStream(decrypted));
                loaded = true;
            }
        } catch (Exception e) {
            System.err.println("Failed to load encrypted gmail.properties: " + e.getMessage());
        }

        // 2. Fallback to plain file (if it exists)
        if (!loaded) {
            try (InputStream in = EmailUtil.class.getResourceAsStream(
                    "/raven/modal/demo/data/gmail.properties")) {
                if (in == null) throw new RuntimeException(
                        "Neither encrypted nor plain gmail.properties found!");
                props.load(in);
            } catch (Exception e) {
                throw new RuntimeException("Cannot load gmail credentials", e);
            }
        }

        GMAIL_USERNAME = props.getProperty("gmail.username");
        APP_PASSWORD   = props.getProperty("gmail.apppassword");

        if (GMAIL_USERNAME == null || APP_PASSWORD == null) {
            throw new RuntimeException(
                    "gmail.username or gmail.apppassword missing");
        }
    }

    public static void sendVerificationCode(String toEmail, String code) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(GMAIL_USERNAME, APP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(GMAIL_USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Password Reset Code");
            message.setText("Your password reset code is: " + code + "\n\nThis code expires in 10 minutes.");

            Transport.send(message);
            System.out.println("Verification email sent to " + toEmail);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    public static void sendEmail(String to, String subject, String body) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(GMAIL_USERNAME, APP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(GMAIL_USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }
}