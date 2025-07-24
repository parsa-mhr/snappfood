package org.example.DAO;


import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.Models.Wallet;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.math.BigDecimal;

public class WalletDAO {
    public final SessionFactory sessionFactory;

    public WalletDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Wallet findByUserId(Long userId) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Wallet w WHERE w.user.id = :userId", Wallet.class)
                    .setParameter("userId", userId)
                    .uniqueResult();
        }
    }

    public void save(Wallet wallet) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.persist(wallet);
            session.getTransaction().commit();
        }
    }

    public void update(Wallet wallet) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.merge(wallet);
            session.getTransaction().commit();
        }
    }

    public BigDecimal findBalanceByUserId(Long userId) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "SELECT w.balance FROM Wallet w WHERE w.user.id = :userId";
            return session.createQuery(hql, BigDecimal.class)
                    .setParameter("userId", userId)
                    .uniqueResultOptional()
                    .orElse(BigDecimal.ZERO); // اگر موجود نبود، صفر برگرداند
        }
    }

}


