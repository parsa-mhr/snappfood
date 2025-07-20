package org.example.Services;

import org.example.Models.Coupon;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;

public class CouponService {
    private final SessionFactory factory = new Configuration().configure().buildSessionFactory();

    public List<Coupon> listAll() {
        try (Session s = factory.openSession()) {
            return s.createQuery("FROM Coupon", Coupon.class).list();
        }
    }

    public Optional<Coupon> applyCoupon(String code) {
        try (Session s = factory.openSession()) {
            return Optional.ofNullable(s.get(Coupon.class, code));
        }
    }
    public Optional<Coupon> findByCode(String code) {
        try (Session session = factory.openSession()) {
            Query<Coupon> query = session.createQuery(
                    "FROM Coupon c WHERE c.coupon_code = :code", Coupon.class);
            query.setParameter("code", code);
            return query.uniqueResultOptional();
        }
    }

}
