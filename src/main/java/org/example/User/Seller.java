package org.example.User;

import jakarta.persistence.*;
import org.example.Restaurant.MenuItem;
import org.example.Restaurant.Restaurant;
import org.hibernate.Session;

import java.util.List;

@Entity
@Table(name = "Sellers")
public class Seller extends User {
    @Id
    private Long id;
    @Transient
    @OneToMany
    private List<Restaurant> restaurants;
    public Seller(String fullName, String email, String password, String phonenumber, String adress) {
        super(fullName, email, password, phonenumber, adress);
        this.setRole(UserRole.seller); // تعیین نقش
    }

    public Seller() {
        super();
        this.setRole(UserRole.seller);
    }
    public List<Restaurant> getRestaurants(Session session) {
        String hql = "FROM Resturants r WHERE r.seller_id = :sid";
        restaurants = session.createQuery(hql, Restaurant.class)
                .setParameter("sid", this.id)
                .list();
        return restaurants;
    }
}
