package org.example.DAO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.Details.Cart;
import org.example.Models.Transaction;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class TransactionDAO {
    private final SessionFactory sessionFactory;

    public TransactionDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void save(Transaction transaction) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.persist(transaction);
            session.getTransaction().commit();
        }
    }

    public List<Transaction> findByUserId(Long userId) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Transaction t WHERE t.user.id = :userId", Transaction.class)
                    .setParameter("userId", userId)
                    .list();
        }
    }
    public List<Transaction> findAll(){
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Transaction", Transaction.class).list();
        }
    }
}