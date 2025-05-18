package org.example.User;

import java.util.ArrayList;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false)
    protected String name;
    @Column(nullable = false)//
    protected String lastname;//
    @Column(unique = true, nullable = false)
    protected String email;
    @Column(nullable = false)
    protected String password;

    public User(String name, String familyName, String email, String password) {
        this.name = name;
        this.lastname = familyName;//
        this.email = email;
        this.password = password;
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
