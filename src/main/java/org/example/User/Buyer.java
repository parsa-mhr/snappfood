package org.example.User;
import jakarta.persistence.*;
import org.example.Details.Cart;

import java.util.ArrayList;
import java.util.List;
@Entity
@Table (name = "Buyers")
public class Buyer extends User {
    // the buyer can add anything to cart
    /* any restaurant cart is unique
    * the status of cart will say what will happen
    * 1. method prev_orders returns a list of carts
    *       it gets every carts with status "delivered" and buyer_id of this buyer
    * 2. method pending_carts returs a list of carts
    *       it gets every carts with status "pending" and belike first method
    * 3. method getCurrent_order will returns a single cart with status "Current_order "
    *       status will be update in payments
    * 4. these will be queries to db and no need to external table for every costumer
    *
    * */
    public Buyer(String name, String lastname , String email, String password , String phone) {
        super(name , lastname , email , password , phone);
    }
}
