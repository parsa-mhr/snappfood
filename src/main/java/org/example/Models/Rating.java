package org.example.Models;

import jakarta.persistence.*;
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
    private Integer score;
    private String comment;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getBuyerId() { return buyerId; }
    public void setBuyerId(Integer buyerId) { this.buyerId = buyerId; }
    public MenuItem getItem() { return item; }
    public void setItem(MenuItem item) { this.item = item; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}