package org.example.User;

import jakarta.persistence.*;

@Entity
@Table(name = "Sellers")
public class Seller extends User {
    @Id
    private Long id;
    @Column(nullable = false)
    private String shopName;

    public Seller(String fullName, String email, String password, String phonenumber, String shopName, String adress) {
        super(fullName, email, password, phonenumber, adress);
        this.shopName = shopName;
        this.setRole(UserRole.seller); // تعیین نقش
    }

    public Seller() {
        super();
        this.setRole(UserRole.seller);
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }
}
