import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.util.Properties;

public class EncryptProperties {
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.setProperty("mysql.host", "mysql-dbase-aclc-research-project.l.aivencloud.com");
        props.setProperty("mysql.port", "15623");
        props.setProperty("mysql.db", "defaultdb");
        props.setProperty("mysql.user", "avnadmin");
        props.setProperty("mysql.password", "AVNS_soxdH3b3zlcf2OSoxIV");
        props.setProperty("mysql.main.table", "aclc_research_titles");
        props.setProperty("mysql.section.table", "best_section_ict_c");
        props.setProperty("mysql.users.table", "users");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        props.store(baos, null);
        byte[] plain = baos.toByteArray();

        SecretKeySpec keySpec = new SecretKeySpec("16ByteSecretKey!".getBytes("UTF-8"), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(plain);

        // output path FROM PROJECT ROOT
        String outputPath = "demo/src/main/resources/raven/modal/demo/data/db.properties.enc";
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            fos.write(encrypted);
        }
        System.out.println("Encrypted file created at: " + outputPath);
    }
}