package org.example.Services;

import org.example.Models.ItemsFilter;
import org.example.Restaurant.MenuItem;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;

public class ItemService {
    private final SessionFactory factory = new Configuration().configure().buildSessionFactory();

    public List<MenuItem> findByFilter(ItemsFilter filter) {
        try (Session s = factory.openSession()) {
            String hql = "SELECT DISTINCT mi FROM MenuItem mi";

            // فقط اگه keywords غیرخالی باشه، جوین اضافه بشه
            if (filter.getKeywords() != null && !filter.getKeywords().isEmpty()) {
                hql += " LEFT JOIN mi.keywords k";
            }
            hql += " WHERE 1=1";


            // اضافه کردن شرط‌ها
            if (filter.getSearch() != null && !filter.getSearch().trim().isEmpty()) {
                hql += " AND mi.name LIKE :search";
            }
            if (filter.getPrice() != null) {
                hql += " AND mi.price <= :price";
            }
            if (filter.getKeywords() != null && !filter.getKeywords().isEmpty()) {
                hql += " AND k IN :keywords";
            }

            Query<MenuItem> q = s.createQuery(hql, MenuItem.class);

            // تنظیم پارامترها
            if (filter.getSearch() != null && !filter.getSearch().trim().isEmpty()) {
                q.setParameter("search", "%" + filter.getSearch().trim() + "%");
            }
            if (filter.getPrice() != null) {
                q.setParameter("price", filter.getPrice());
            }
            if (filter.getKeywords() != null && !filter.getKeywords().isEmpty()) {
                q.setParameter("keywords", filter.getKeywords());
            }

            List<MenuItem> result = q.getResultList();
            System.out.println("Found " + result.size() + " menu items");
            return result;
        } catch (Exception e) {
            System.err.println("Error in searchMenuItems: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }


    public Optional<MenuItem> getById(int id) {
        try (Session s = factory.openSession()) {
            return Optional.ofNullable(s.get(MenuItem.class, id));
        }
    }
}
