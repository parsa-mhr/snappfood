package org.example.Restaurant;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * کلاس MenuCategory برای مدل‌سازی دسته‌بندی‌های منوی رستوران
 * این کلاس با endpointهای /restaurants/{id}/menu و /vendors/{id} همخوانی دارد
 */
@Entity
@Table(name = "menus", uniqueConstraints = @UniqueConstraint(columnNames = { "restaurant_id", "title" }))
public class MenuCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(nullable = false)
    private String title; // عنوان دسته‌بندی منو

    @ManyToOne
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant; // رستوران مرتبط

    @ManyToMany
    @JoinTable(
        name = "menu_items_mapping",
        joinColumns = @JoinColumn(name = "menu_id"),
        inverseJoinColumns = @JoinColumn(name = "menu_item_id")
    )
    private List<MenuItem> items = new ArrayList<>(); // آیتم‌های مرتبط با این دسته‌بندی

    public MenuCategory() {
    }

    public MenuCategory(String title, Restaurant restaurant) {
        this.title = title;
        this.restaurant = restaurant;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public List<MenuItem> getItems() {
        return items;
    }

    public void setItems(List<MenuItem> items) {
        this.items = items;
    }

    public void addItem(MenuItem item) {
        this.items.add(item);
    }

    public void removeItem(MenuItem item) {
        this.items.remove(item);
    }
}
