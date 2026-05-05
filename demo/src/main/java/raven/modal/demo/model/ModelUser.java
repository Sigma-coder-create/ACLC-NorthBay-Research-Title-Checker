package raven.modal.demo.model;

public class ModelUser {

    private String userName;
    private String mail;
    private Role role;
    private String avatarPath;   // NEW: path to custom avatar image (jpg, png, svg, etc.)

    public ModelUser(String userName, String mail, Role role) {
        this.userName = userName;
        this.mail = mail;
        this.role = role;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    // NEW getter and setter for avatarPath
    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    public enum Role {
        TEACHER, STUDENT;

        @Override
        public String toString() {
            return this == TEACHER ? "Teacher" : "Student";
        }
    }
}