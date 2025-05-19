package org.example.User;

import jakarta.persistence.*;

@Entity
@Table(name = "Courier")
public class Courier extends User {
    @Id
    private Long id;
    @Column(nullable = false, unique = true)
    String bankinformation;

    public Courier(String name, String lastname, String email, String password, String phonenumber,
            String bankinformation) {
        super(name, lastname, email, password, phonenumber);
        this.bankinformation = bankinformation;
    }

    public Courier() {
        super("", "", "", "", "");

    }

    public String getbankinformation() {
        return bankinformation;
    }

    public void setbankinformation(String bankinformation) {
        this.bankinformation = bankinformation;
    }


}
