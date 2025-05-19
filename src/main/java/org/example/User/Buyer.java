package org.example.User;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.example.Details.Cart;

import java.util.ArrayList;
import java.util.List;
@Entity
@Table (name = "Buyers")
public class Buyer extends User {
    private  List<Cart> prev_orders = new ArrayList<Cart>();
    private List<Cart> Carts = new ArrayList<>() ;
    private Cart current_order ;

    public List<Cart> getPrev_orders() {
        return prev_orders;
    }
    public void setPrev_orders(List<Cart> prev_orders) {
        this.prev_orders = prev_orders;
    }
    public List<Cart> getCarts() {
        return Carts;
    }
    public void order_paid (Cart cart) {
        for (Cart cart1 : Carts) {
            if (cart1.equals(cart)) {
                current_order = cart1;
                Carts.remove(current_order);
                prev_orders.add(current_order);
            }
        }
    }
}
