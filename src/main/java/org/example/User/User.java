package org.example.User;

import java.util.ArrayList;
import jakarta.persistence.*;
//import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false)
    protected String name;
    @Column(nullable = false) //
    protected String lastname;//
    @Column(unique = true, nullable = false)//
    protected String email;
    @Column(nullable = false)
    protected String password;
    @Column(unique = true, nullable = false)
    protected String phonenumber;
    @Lob
    @Column(name = "image", columnDefinition = "LONGBLOB")
    byte[] Image;

    public User(String name, String familyName, String email, String password, String phonenumber) {
        this.name = name;
        this.lastname = familyName;//
        this.email = email;
        this.password = password;
        this.phonenumber = phonenumber;//
    }

    public User() {

    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setfamilyName(String familyName) {
        this.lastname = familyName;
    }//

    public String getfamilyName() {
        return lastname;
    }//

    public String getphonenumber() {
        return phonenumber;
    }

    public void setphonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
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
}
