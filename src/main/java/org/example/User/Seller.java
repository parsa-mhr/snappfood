package org.example.User;

import jakarta.persistence.*;

@Entity
@Table(name = "Sellers")
public class Seller extends User {
    @Id
    private Long id;
    @Column(nullable = false)
    String Shopname;

    public Seller(String name, String lastname, String email, String password, String Shopname,String phonenumber) {
        super(name, lastname, email, password,phonenumber);
        Shopname = Shopname;
    }

    public Seller() {
        super("","", "", "" , "");
    }

    public void setShopName(String Shopname) {
        this.Shopname = Shopname;
    }

    public void setImage(byte[] ShopImage) {
        this.Image = ShopImage;
    }

    public String getShopname() {
        return Shopname;
    }

    public byte[] getShopImage() {
        return Image;
    }
}
