package org.example.DAO;

import jakarta.persistence.EntityManager;
import org.example.Details.Cart;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class OrderDAO {
    public final SessionFactory sessionFactory;

    public OrderDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Cart findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                            "SELECT c FROM Cart c " +
                                    "JOIN FETCH c.buyer " +
                                    "JOIN FETCH c.restaurant " +
                                    "LEFT JOIN FETCH c.coupon " +
                                    "JOIN FETCH c.items i " +
                                    "JOIN FETCH i.menuItem " +
                                    "WHERE c.id = :id",
                            Cart.class)
                    .setParameter("id", id)
                    .getSingleResult();
        }
    }

    public List<Cart> findAll() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                            "SELECT c FROM Cart c " +
                                    "JOIN FETCH c.buyer " +
                                    "JOIN FETCH c.restaurant " +
                                    "LEFT JOIN FETCH c.coupon " +
                                    "JOIN FETCH c.items i " +
                                    "JOIN FETCH i.menuItem",
                            Cart.class)
                    .list();
        }
    }

    public void update(Cart order) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.merge(order);
            session.getTransaction().commit();
        }
    }
}