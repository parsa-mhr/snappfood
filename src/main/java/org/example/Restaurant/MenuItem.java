package org.example.Restaurant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.Base64;
import java.util.List;

/**
 * کلاس MenuItem برای مدل‌سازی آیتم‌های منوی رستوران
 * این کلاس با schema food_item در مشخصات OpenAPI همخوانی دارد
 */
@Entity
@Table(name = "menu_items")
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "LONGTEXT")
    private String imageBase64;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int supply;

    @ElementCollection
    @CollectionTable(name = "menu_item_keywords", joinColumns = @JoinColumn(name = "menu_item_id"))
    @Column(name = "keyword", nullable = false)
    @JsonIgnore
    public List<String> keywords;

    @ManyToOne
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    public MenuItem() {
    }

    public MenuItem(String name, String description, int price, int supply, List<String> keywords, Restaurant restaurant) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.supply = supply;
        this.keywords = keywords;
        this.restaurant = restaurant;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public void setImage(byte[] image) {
        this.imageBase64 = (image != null) ? Base64.getEncoder().encodeToString(image) : null;
    }
    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
    public int getSupply() { return supply; }
    public void setSupply(int supply) { this.supply = supply; }
    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    public Restaurant getRestaurant() { return restaurant; }
    public void setRestaurant(Restaurant restaurant) { this.restaurant = restaurant; }
}
