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
    public boolean removeRating(int Id) {
        try (Session s = factory.openSession()) {
            s.beginTransaction();
            Query<?> q = s.createQuery("DELETE Rating r WHERE r.id = :id ");
            q.setParameter("id", Id);
            int count = q.executeUpdate();
            s.getTransaction().commit();
            return count>0 ;

        }
    }
    public Rating updateRating(int rating , String comment , String imageBase64 , int id) {
        Rating x = getById(id).get() ;
        if (x != null) {
            x.setComment (comment );
            x.setScore(rating);
            x.setImageBase64 (imageBase64);
        }

        try (Session s = factory.openSession()) {
            s.update(x);
            s.getTransaction().commit();
            return x;
        }
    }
}
