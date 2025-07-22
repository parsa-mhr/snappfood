package org.example.Models;

import java.util.List;

public class RatingResponseDto {
    private Integer id;
    private List<Long> item_ids;
    private Integer rating;
    private String comment;
    private List<String> imageBase64;
    private Integer user_id;
    private String created_at;

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public List<Long> getItem_ids() {
        return item_ids;
    }

    public void setItem_ids(List<Long> item_ids) {
        this.item_ids = item_ids;
    }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public List<String> getImageBase64() { return imageBase64; }
    public void setImageBase64(List<String> imageBase64) { this.imageBase64 = imageBase64; }

    public Integer getUser_id() { return user_id; }
    public void setUser_id(Integer user_id) { this.user_id = user_id; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
}
