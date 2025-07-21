package org.example.User;

import jakarta.persistence.*;
import org.example.Restaurant.Restaurant;

import java.util.List;

@Entity
@Table(name = "sellers")
public class Seller extends User {

    @OneToMany(mappedBy = "seller", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Restaurant> restaurants;

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
