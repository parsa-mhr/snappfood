package org.example.User;

import jakarta.persistence.*;
import org.example.Security.PasswordUtil;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

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

    @Column
    protected String status; // approved, rejected

    @Column(name = "created_at")
    protected LocalDateTime createdAt = LocalDateTime.now();

    public User(String fullName, String email, String password, String phonenumber, String adress) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.phonenumber = phonenumber;
        this.adress = adress;
    }

    public User() {
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAdress() {
        return adress;
    }

    public void setAdress(String adress) {
        this.adress = adress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getId() {
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
