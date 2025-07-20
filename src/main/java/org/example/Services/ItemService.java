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
            String hql = "FROM MenuItem i WHERE 1=1";


            if (filter.getSearch() != null)
                hql += " AND i.name LIKE :search";

            if (filter.getPrice() != null)
                hql += " AND i.price <= :price";

            if (filter.getKeywords() != null && !filter.getKeywords().isEmpty())
                hql += " AND i.keywords IN (:kw)";  // فرض بر این است که فیلد keywords از نوع `String` یا مجموعه‌ای از تگ‌ها است

            Query<MenuItem> q = s.createQuery(hql, MenuItem.class);

            if (filter.getSearch() != null)
                q.setParameter("search", "%" + filter.getSearch() + "%");

            if (filter.getPrice() != null)
                q.setParameter("price", filter.getPrice());

            if (filter.getKeywords() != null && !filter.getKeywords().isEmpty())
                q.setParameter("kw", filter.getKeywords());

            return q.list();
        }
    }


    public Optional<MenuItem> getById(int id) {
        try (Session s = factory.openSession()) {
            return Optional.ofNullable(s.get(MenuItem.class, id));
        }
    }
}
