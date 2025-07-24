package org.example.User;

import jakarta.persistence.*;
import org.example.Restaurant.Restaurant;

import java.util.List;

@Entity
@Table(name = "sellers")
public class Seller extends User {

    @OneToMany(mappedBy = "seller", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Restaurant> restaurants;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "bankName", column = @Column(name = "bank_name")),
            @AttributeOverride(name = "accountNumber", column = @Column(name = "account_number"))
    })
    private BankInfo bankInformation;

    public BankInfo getBankInformation() {
        return bankInformation;
    }

    public void setBankInformation(BankInfo bankInformation) {
        this.bankInformation = bankInformation;
    }

    public Seller() {
        super();
        this.setRole(UserRole.seller);
    }

    public Seller(String fullName, String email, String password, String phonenumber, String adress) {
        super(fullName, email, password, phonenumber, adress);
        this.setRole(UserRole.seller);
    }

    public List<Restaurant> getRestaurants() {
        return restaurants;
    }

    public void setRestaurants(List<Restaurant> restaurants) {
        this.restaurants = restaurants;
    }
}
