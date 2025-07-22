package org.example.Services;

import org.example.Models.VendorFilter;
import org.example.Restaurant.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import java.util.List;

public class RestaurantService {
    private final SessionFactory sessionFactory;

    public RestaurantService() {
        sessionFactory = new Configuration().configure().buildSessionFactory();
    }

    public List<Restaurant> findByFilter(VendorFilter filter) {
        try (Session session = sessionFactory.openSession()) {
            StringBuilder hql = new StringBuilder("FROM Restaurant r WHERE 1=1");

            if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                hql.append(" AND (r.name LIKE :search OR r.address LIKE :search)");
            }

            if (filter.getKeywords() != null && !filter.getKeywords().isEmpty()) {
                hql.append(" AND (");
                for (int i = 0; i < filter.getKeywords().size(); i++) {
                    hql.append("r.name LIKE :kw").append(i);
                    if (i < filter.getKeywords().size() - 1) {
                        hql.append(" OR ");
                    }
                }
                hql.append(")");
            }

            Query<Restaurant> query = session.createQuery(hql.toString(), Restaurant.class);

            if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                query.setParameter("search", "%" + filter.getSearch() + "%");
            }

            if (filter.getKeywords() != null && !filter.getKeywords().isEmpty()) {
                for (int i = 0; i < filter.getKeywords().size(); i++) {
                    query.setParameter("kw" + i, "%" + filter.getKeywords().get(i) + "%");
                }
            }

            return query.list();
        }
    }


    public List<MenuItem> getById(int id) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("SELECT mi FROM MenuItem mi WHERE mi.restaurant.id = :restaurantId", MenuItem.class)
                    .setParameter("restaurantId", id)
                    .getResultList();
        }
    }
    public Restaurant getRestaurantById(int id) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(Restaurant.class, id);
        }
    }
}