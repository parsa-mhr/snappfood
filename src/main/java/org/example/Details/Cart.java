package org.example.Details;

import jakarta.persistence.*;
import org.example.User.Buyer;
import org.example.User.User;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

@Entity
@Table (name = "Carts")
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long cart_id;
    // arraylist of foods (when food class includes)
    // Restaurant restaurant ;
    @ManyToOne
    @JoinColumn(name = "User" , nullable = false , referencedColumnName = "id")
    Buyer buyer;
    @Enumerated(EnumType.STRING)
    OrderStatus Status ;
    public Cart(Buyer buyer) {
        this.buyer = buyer;
        this.Status = OrderStatus.Pending;
        //this.restaurant = restaurant //import to constructor
    }

    public Cart() {

    }
    public void setStatus(OrderStatus Status , SessionFactory sessionFactory) {
        this.Status = Status;
        Transaction transaction = sessionFactory.getCurrentSession().beginTransaction();
        sessionFactory.getCurrentSession().saveOrUpdate(this);
        transaction.commit();
        //deliverd , pending , current order
    }
}
