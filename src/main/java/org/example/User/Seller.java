package org.example.User;

import jakarta.persistence.*;

@Entity
@Table(name = "Sellers")
public class Seller extends User {
    @Id
    private Long id;

    public Seller(String fullName, String email, String password, String phonenumber,String adress) {
        super(fullName, email, password, phonenumber, adress);
        this.setRole(UserRole.seller); // تعیین نقش
    }

    public Seller() {
        super();
        this.setRole(UserRole.seller);
    }

}
