package org.example.Models;

import org.example.Details.Coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CouponResponseDto {
    private Long id;
    private String code;
    private BigDecimal discount;
    private BigDecimal min_price ;
    private String discountType;
    private Integer maxUses;
    private Integer usedCount;
    private String expirationDate;
    private String status;
    private String createdAt;

    public CouponResponseDto(Coupon coupon) {
        this.id = coupon.getId();
        this.code = coupon.getCode();
        this.discount = coupon.getDiscount();
        this.discountType = coupon.getDiscountType();
        this.maxUses = coupon.getMaxUses();
        this.usedCount = coupon.getUsedCount();
        this.expirationDate = coupon.getExpirationDate() != null ?
                coupon.getExpirationDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
        this.status = coupon.getStatus();
        this.createdAt = coupon.getCreatedAt() != null ?
                coupon.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
        this.min_price = coupon.getMin_price();
    }

    public BigDecimal getMin_price() {
        return min_price;
    }

    public void setMin_price(BigDecimal min_price) {
        this.min_price = min_price;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }
    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }
    public Integer getMaxUses() { return maxUses; }
    public void setMaxUses(Integer maxUses) { this.maxUses = maxUses; }
    public Integer getUsedCount() { return usedCount; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }
    public String getExpirationDate() { return expirationDate; }
    public void setExpirationDate(String expirationDate) { this.expirationDate = expirationDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}