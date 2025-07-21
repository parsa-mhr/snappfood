package org.example.Details;

import java.util.List;

public class OrderDTO {
    private final Long id;
    private final String deliveryAddress;
    private final Long customerId;
    private final Long vendorId;
    private final Long couponId;
    private final List<Long> itemIds;
    private final int rawPrice;
    private final int taxFee;
    private final int additionalFee;
    private final int courierFee;
    private final int payPrice;
    private final Long courierId;
    private final String status;
    private final String createdAt;
    private final String updatedAt;

    public OrderDTO(Long id, String deliveryAddress, Long customerId, Long vendorId, Long couponId,
                    List<Long> itemIds, long rawPrice, int taxFee, int additionalFee, int courierFee,
                    long payPrice, Long courierId, String status, String createdAt, String updatedAt) {
        this.id = id;
        this.deliveryAddress = deliveryAddress;
        this.customerId = customerId;
        this.vendorId = vendorId;
        this.couponId = couponId;
        this.itemIds = itemIds;
        this.rawPrice = Math.toIntExact(rawPrice);
        this.taxFee = taxFee;
        this.additionalFee = additionalFee;
        this.courierFee = courierFee;
        this.payPrice = Math.toIntExact(payPrice);
        this.courierId = courierId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // getterها
    public Long getId() {
        return id;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public Long getCouponId() {
        return couponId;
    }

    public List<Long> getItemIds() {
        return itemIds;
    }

    public int getRawPrice() {
        return rawPrice;
    }

    public int getTaxFee() {
        return taxFee;
    }

    public int getAdditionalFee() {
        return additionalFee;
    }

    public int getCourierFee() {
        return courierFee;
    }

    public int getPayPrice() {
        return payPrice;
    }

    public Long getCourierId() {
        return courierId;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() { return updatedAt; }
}