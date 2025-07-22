package org.example.Models;

import org.example.Details.Cart;
import org.example.Details.CartItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderResponseDto {
    public long id;
    public String delivery_address;
    public long customer_id;
    public long vendor_id;
    public Long coupon_id;
    public Map<Long , Integer> item_ids;
    public double raw_price;
    public double tax_fee;
    public double additional_fee;
    public double courier_fee;
    public double pay_price;
    public Long courier_id;
    public String status;
    public String created_at;
    public String updated_at;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDelivery_address() {
        return delivery_address;
    }

    public void setDelivery_address(String delivery_address) {
        this.delivery_address = delivery_address;
    }

    public long getCustomer_id() {
        return customer_id;
    }

    public void setCustomer_id(long customer_id) {
        this.customer_id = customer_id;
    }

    public long getVendor_id() {
        return vendor_id;
    }

    public void setVendor_id(long vendor_id) {
        this.vendor_id = vendor_id;
    }

    public Long getCoupon_id() {
        return coupon_id;
    }

    public void setCoupon_id(Long coupon_id) {
        this.coupon_id = coupon_id;
    }

    public Map<Long, Integer> getItem_ids() {
        return item_ids;
    }

    public void setItem_ids(Map<Long , Integer> item_ids) {
        this.item_ids = item_ids;
    }

    public double getRaw_price() {
        return raw_price;
    }

    public void setRaw_price(double raw_price) {
        this.raw_price = raw_price;
    }

    public double getTax_fee() {
        return tax_fee;
    }

    public void setTax_fee(double tax_fee) {
        this.tax_fee = tax_fee;
    }

    public double getAdditional_fee() {
        return additional_fee;
    }

    public void setAdditional_fee(double additional_fee) {
        this.additional_fee = additional_fee;
    }

    public double getCourier_fee() {
        return courier_fee;
    }

    public void setCourier_fee(double courier_fee) {
        this.courier_fee = courier_fee;
    }

    public double getPay_price() {
        return pay_price;
    }

    public void setPay_price(double pay_price) {
        this.pay_price = pay_price;
    }

    public Long getCourier_id() {
        return courier_id;
    }

    public void setCourier_id(Long courier_id) {
        this.courier_id = courier_id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }

    public OrderResponseDto(Cart cart) {
        this.id = id;
        this.delivery_address = cart.getDelivery_address();
        this.customer_id = cart.getBuyer() == null ? null :cart.getBuyer().getId();
        this.vendor_id = cart.getRestaurant() == null ? null :cart.getRestaurant().getId();
        this.coupon_id = cart.getCoupon() == null ? null : cart.getCoupon().getId();
        Map<Long , Integer> list = new HashMap<>() ;

        for (CartItem item : cart.getItems())
            list.put(item.getId() , item.getQuantity());

        this.item_ids = list;
        this.raw_price = cart.getPay_price();
        this.tax_fee = cart.getRestaurant().getTaxFee();
        this.additional_fee = cart.getRestaurant() == null ? null : cart.getRestaurant().getAdditionalFee();
        this.courier_fee = 30000;
        this.pay_price = cart.getPay_price();
        this.courier_id = Long.valueOf(cart.getCourier_Id());
        this.status = String.valueOf(cart.getStatus());
        this.created_at = String.valueOf(cart.getCreatedAt());
        this.updated_at = String.valueOf(cart.getUpdatedAt());
    }
    public OrderResponseDto (){}
}
