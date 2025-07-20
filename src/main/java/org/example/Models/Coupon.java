package org.example.Models;

import jakarta.persistence.*;

import java.sql.Date;

@Entity
@Table(name = "coupons")
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String coupon_code;
    private coupontype type;
    private long value ;
    private long user_count ;
    private Date start_date;
    private Date end_date;

    public String getCoupon_code() {
        return coupon_code;
    }

    public void setCoupon_code(String coupon_code) {
        this.coupon_code = coupon_code;
    }

    public coupontype getType() {
        return type;
    }

    public void setType(coupontype type) {
        this.type = type;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public long getUser_count() {
        return user_count;
    }

    public void setUser_count(long user_count) {
        this.user_count = user_count;
    }

    public Date getStart_date() {
        return start_date;
    }

    public void setStart_date(Date start_date) {
        this.start_date = start_date;
    }

    public Date getEnd_date() {
        return end_date;
    }

    public void setEnd_date(Date end_date) {
        this.end_date = end_date;
    }

    public Long getId() {
        return id ;
    }
}