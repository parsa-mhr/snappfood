package org.example.User;

import jakarta.persistence.*;

@Entity
@PrimaryKeyJoinColumn(name = "id") // این خط ضروریه برای ارتباط کلید اصلی با کلاس پدر
@Table(name = "Sellers")
public class Seller extends User {

    public Seller(String fullName, String email, String password, String phonenumber, String adress) {
        super(fullName, email, password, phonenumber, adress);
        this.setRole(UserRole.seller);
    }

    public Seller() {
        super();
        this.setRole(UserRole.seller);
    }
}
