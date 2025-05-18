package org.example.User;

import jakarta.persistence.*;

@Entity
@Table(name = "Sellers")
public class Seller extends User {
    @Id
    private Long id;
    @Column(nullable = false)
    String Shopname;
    @Lob
    @Column(name = "image", columnDefinition = "LONGBLOB")
    byte[] ShopImage;

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

    public void setShopImage(byte[] ShopImage) {
        this.ShopImage = ShopImage;
    }

    public String getShopname() {
        return Shopname;
    }

    public byte[] getShopImage() {
        return ShopImage;
    }
}
