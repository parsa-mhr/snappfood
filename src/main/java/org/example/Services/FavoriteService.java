package org.example.Services;

import org.example.Models.Favorite;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import java.util.List;

public class FavoriteService {
    private final SessionFactory factory = new Configuration().configure().buildSessionFactory();

    public List<Favorite> listFavorites(int buyerId) {
        try (Session s = factory.openSession()) {
            Query<Favorite> q = s.createQuery("FROM Favorite f WHERE f.buyerId = :bid", Favorite.class);
            q.setParameter("bid", buyerId);
            return q.list();
        }
    }

    public Favorite addFavorite(Favorite fav) {
        try (Session s = factory.openSession()) {
            s.beginTransaction();
            s.save(fav);
            s.getTransaction().commit();
            return fav;
        }
    }

    public boolean removeFavorite(int buyerId, int restaurantId) {
        try (Session s = factory.openSession()) {
            s.beginTransaction();
            Query<?> q = s.createQuery("DELETE Favorite f WHERE f.buyerId = :bid AND f.restaurant.id = :rid");
            q.setParameter("bid", buyerId);
            q.setParameter("rid", restaurantId);
            int count = q.executeUpdate();
            s.getTransaction().commit();
            return count > 0;
        }
    }
}
