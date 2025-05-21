package org.example.User;

import jakarta.persistence.*;
import org.example.Security.PasswordUtil;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    protected String fullName;

    @Column(unique = true, nullable = true)
    protected String email;

    @Column(nullable = false)
    protected String password;

    @Column(unique = true, nullable = false)
    protected String phonenumber;

    @Lob
    @Column(name = "image", columnDefinition = "LONGBLOB")
    protected byte[] image;

    @Transient // چون فقط برای API هست و قرار نیست مستقیم در دیتابیس ذخیره بشه
    private String profileImageBase64;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    protected UserRole role; // 🔸 نقش کاربر: buyer, seller, courier

    @Column(nullable = true)
    protected String adress;

    @Column(nullable = true)
    protected String bankName;

    @Column(nullable = true)
    protected String accountNumber;

    public User(String fullName, String email, String password, String phonenumber, String adress) {
        this.fullName = fullName;
        this.email = email;
        this.password = PasswordUtil.hashPassword(password);
        this.phonenumber = phonenumber;
        this.adress = adress;
    }
    public User(String fullName, String password, String phonenumber, String adress) {
        this.fullName = fullName;
        this.email = null;
        this.password = PasswordUtil.hashPassword(password);
        this.phonenumber = phonenumber;
        this.adress = adress;
    }

    public User() {
    }

    public long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getadress() {
        return adress;
    }

    public void setadress(String adress) {
        this.adress = adress;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public String getProfileImageBase64() {
        return profileImageBase64;
    }

    public void setProfileImageBase64(String profileImageBase64) {
        this.profileImageBase64 = profileImageBase64;
    }
}
