package org.example.Models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CouponRequestDto {
    private String coupon_code;
    private BigDecimal value;
    private String type;
    private Integer user_count;
    private String end_date;
    private String start_date ;
    private BigDecimal min_price ;// به صورت String برای JSON


    public String getCoupon_code() {
        return coupon_code;
    }

    public void setCoupon_code(String coupon_code) {
        this.coupon_code = coupon_code;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getUser_count() {
        return user_count;
    }

    public void setUser_count(Integer user_count) {
        this.user_count = user_count;
    }

    public String getEnd_date() {
        return end_date;
    }

    public void setEnd_date(String end_date) {
        this.end_date = end_date;
    }

    public String getStart_date() {
        return start_date;
    }

    public void setStart_date(String start_date) {
        this.start_date = start_date;
    }

    public BigDecimal getMin_price() {
        return min_price;
    }

    public void setMin_price(BigDecimal min_price) {
        this.min_price = min_price;
    }

    public String getCode() { return coupon_code; }
    public void setCode(String code) { this.coupon_code = code; }
    public BigDecimal getDiscount() { return value; }
    public void setDiscount(BigDecimal discount) { this.value = discount; }
    public String getDiscountType() { return type; }
    public void setDiscountType(String discountType) { this.type = discountType; }
    public Integer getMaxUses() { return user_count; }


}