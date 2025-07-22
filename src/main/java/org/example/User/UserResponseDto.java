package org.example.User;

import org.example.User.User;
import org.example.User.UserRole;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserResponseDto {
    private Long id;
    private String fullName;
    private String email;
    private String phonenumber;
    private String adress;
    private String status;
    private UserRole role;
    private String createdAt;

    public UserResponseDto(User user) {
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.phonenumber = user.getPhonenumber();
        this.adress = user.getAdress();
        this.status = user.getStatus();
        this.role = user.getRole();
        this.createdAt = user.getCreatedAt() != null ?
                user.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhonenumber() { return phonenumber; }
    public void setPhonenumber(String phonenumber) { this.phonenumber = phonenumber; }
    public String getAdress() { return adress; }
    public void setAdress(String adress) { this.adress = adress; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}