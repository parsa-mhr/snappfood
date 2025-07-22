package org.example.Models;

import java.util.List;

public class RatingRequest {
    private Integer order_id;
    private Integer rating;
    private String comment;
    private List<String> imageBase64;

    public Integer getOrder_id() {
        return order_id;
    }

    public void setOrder_id(Integer order_id) {
        this.order_id = order_id;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<String> getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(List<String> imageBase64) {
        this.imageBase64 = imageBase64;
    }
}
