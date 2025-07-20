package org.example.Models;

import jakarta.persistence.*;
import org.example.Details.Cart;
import org.example.Restaurant.MenuItem;
import org.hibernate.cache.spi.support.AbstractReadWriteAccess;

@Entity
@Table(name = "ratings")
public class Rating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer buyerId;
    @ManyToOne
    @JoinColumn(name = "item_id")
    private MenuItem item;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "order_id")
    private Cart order_id ;

    private Integer rating;
    private String comment;
    private String imageBase64;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getBuyerId() { return buyerId; }
    public void setBuyerId(Integer buyerId) { this.buyerId = buyerId; }
    public MenuItem getItem() { return item; }
    public void setItem(MenuItem item) { this.item = item; }
    public Integer getScore() { return rating; }
    public void setScore(Integer score) { this.rating = score; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public Cart getOrder_id() {
        return order_id;
    }

    public void setOrder_id(Cart order_id) {
        this.order_id = order_id;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getImageBase64() {
        return imageBase64;
    }
}