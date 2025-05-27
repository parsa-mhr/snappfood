package org.example.Restaurant;

import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.TypedQuery;
import org.example.Main;
import org.hibernate.Session;

import java.util.List;

public class Menu {
    private String title ;
    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
    List<MenuItem> menuitems ;



    public Menu(long restaurantId, Session session) {
        this.menuitems = loadMenuItems(restaurantId, session);
    }

    private List<MenuItem> loadMenuItems(long restaurantId, Session session) {
        String hql = "FROM MenuItem f WHERE f.restaurant = :rid";
        return session.createQuery(hql, MenuItem.class)
                .setParameter("rid", restaurantId)
                .list();
    }

    public List<MenuItem> getItems() {
        return menuitems;
    }
}
