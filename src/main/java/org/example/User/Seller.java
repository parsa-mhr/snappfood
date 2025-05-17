package org.example.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table (name = "Sellers")
public class Seller extends User {
    @Id
    private Long id ;
    @Column (nullable = false)
    String Shopname ;
    public Seller (String name, String email, String password, String Shopname) {
        super (name , email , password);
        Shopname = Shopname;
    }
    public Seller() {
        super("","","");
    }

    public void setShopName(String Shopname) {
        this.Shopname = Shopname;
    }
}
