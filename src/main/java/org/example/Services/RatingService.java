package org.example.Services;

import org.example.Models.Rating;
import org.example.Models.RatingResponseDto;
import org.example.Restaurant.MenuItem;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import org.hibernate.Transaction;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RatingService {
    private final SessionFactory factory = new Configuration().configure().buildSessionFactory();
    public List<Rating> listAll() {
        try (Session s = factory.openSession()) { return s.createQuery("FROM Rating", Rating.class).list(); }
    }
    public List<RatingResponseDto> listByItem(int itemId) {
        try (Session s = factory.openSession()) {
            Query<Rating> q = s.createQuery(
                    "SELECT r FROM Rating r JOIN r.items i WHERE i.id = :iid",
                    Rating.class
            );
            q.setParameter("iid", itemId);
            List<Rating> ratings = q.list();

            // تبدیل لیست به لیست DTO
            return ratings.stream()
                    .map(RatingService::toDto)
                    .collect(Collectors.toList());
        }
    }

    public RatingResponseDto getById(int id) {
        try (Session s = factory.openSession()) {
            Optional<Rating> rate = Optional.ofNullable(s.get(Rating.class, id));
            if(rate.isPresent()) {
                return toDto(rate.get());
            }else
                return null ;
        }
    }

    public Rating addRating(Rating rating) {
        try (Session s = factory.openSession()) {
            s.beginTransaction(); s.save(rating); s.getTransaction().commit(); return rating;
        }
    }
    public boolean removeRating(int Id) {
        try (Session s = factory.openSession()) {
            s.beginTransaction();
            Query<?> q = s.createQuery("DELETE Rating r WHERE r.id = :id ");
            q.setParameter("id", Id);
            int count = q.executeUpdate();
            s.getTransaction().commit();
            return count>0 ;

        }
    }
    public Rating updateRating(Integer rating, String comment, List<String> imageBase64, int id) {
    Rating x = null;

    try (Session s = factory.openSession()) {
        x = s.get(Rating.class, id);
    }

    if (x != null) {
        if (comment != null) {
            x.setComment(comment);
        }

        if (rating != null) {
            x.setScore(rating);
        }

        if (imageBase64 != null && !imageBase64.isEmpty()) {
            x.setImageBase64(imageBase64.get(0));
        }

        try (Session s = factory.openSession()) {
            Transaction tx = s.beginTransaction();
            s.update(x);
            tx.commit();
        }
    }

    return x;
}


    public static RatingResponseDto toDto(Rating rating) {
        RatingResponseDto dto = new RatingResponseDto();
        dto.setId(rating.getId());
        dto.setRating(rating.getRating());
        dto.setComment(rating.getComment());
        dto.setUser_id(rating.getBuyerId());

        List<Long> itemIds = rating.getItems()
                .stream()
                .map(MenuItem::getId)
                .collect(Collectors.toList());
        dto.setItem_ids(itemIds);
        // در فرض نبودن multiple image: یکی رو توی لیست بذار
        dto.setImageBase64(
                rating.getImageBase64() != null ?
                        Collections.singletonList(rating.getImageBase64()) :
                        Collections.emptyList()
        );

        // در صورتی که بخوای از تاریخ سفارش استفاده کنی مثلاً:
        if (rating.getOrder_id() != null && rating.getOrder_id().getCreatedAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            dto.setCreated_at(rating.getOrder_id().getCreatedAt().format(formatter));
        } else {
            dto.setCreated_at(null);
        }

        return dto;
    }
}
