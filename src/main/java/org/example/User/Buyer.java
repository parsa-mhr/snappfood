package org.example.User;

import jakarta.persistence.*;
import org.example.Details.Cart;
import org.example.Restaurant.MenuItem;
import org.hibernate.Session;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Buyers")
public class Buyer extends User {
    // the buyer can add anything to cart
    /*
     * any restaurant cart is unique
     * the status of cart will say what will happen
     * 1. method prev_orders returns a list of carts
     * it gets every carts with status "delivered" and buyer_id of this buyer
     * 2. method pending_carts returs a list of carts
     * it gets every carts with status "pending" and belike first method
     * 3. method getCurrent_order will returns a single cart with status
     * "Current_order "
     * status will be update in payments
     * 4. these will be queries to db and no need to external table for every
     * costumer
     *
     */
    public Buyer(String fullname, String email, String password, String phone,String adress) {
        super(fullname, email, password, phone, adress);
        this.setRole(UserRole.buyer); // تعیین نقش
    }

    public Buyer() {
        super();
        this.setRole(UserRole.buyer);
    }
    public List<Cart> pendingCarts (Session session) {
        String hql = "FROM Cart c WHERE c.buyer = :uid AND c.Status = 'Pending'";
        return session.createQuery(hql, Cart.class)
                .setParameter("uid", this.getId())
                .list();
    }
    public List<Cart> DeliveredCarts (Session session) {
        String hql = "FROM Cart c WHERE c.buyer = :uid AND c.Status = 'Delivered'";
        return session.createQuery(hql , Cart.class).setParameter(":uid" , this.getId()).list();
    }
    public Cart getCurrent_order (Session session) {
            String hql = "FROM Cart c WHERE c.buyer = :uid AND c.Status = 'Current'";
            return (Cart) session.createQuery(hql, Cart.class).uniqueResult();

    }

}
