package org.example.Services;

import org.example.Models.Rating;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;

public class RatingService {
    private final SessionFactory factory = new Configuration().configure().buildSessionFactory();
    public List<Rating> listAll() {
        try (Session s = factory.openSession()) { return s.createQuery("FROM Rating", Rating.class).list(); }
    }
    public List<Rating> listByItem(int itemId) {
        try (Session s = factory.openSession()) {
            Query<Rating> q = s.createQuery("FROM Rating r WHERE r.item.id = :iid", Rating.class);
            q.setParameter("iid", itemId); return q.list();
        }
    }
    public Optional<Rating> getById(int id) {
        try (Session s = factory.openSession()) {
            return Optional.ofNullable(s.get(Rating.class, id));
        }
    }
    public Rating addRating(Rating rating) {
        try (Session s = factory.openSession()) {
            s.beginTransaction(); s.save(rating); s.getTransaction().commit(); return rating;
        }
    }
}
