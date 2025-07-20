package org.example.Models;

import java.util.List;

public class CreateOrderReq {
    public String delivery_address;
    public long vendor_id;
    public Long coupon_id;
    public List<ItemRequest> items;

    public static class ItemRequest {
        public long item_id;
        public int quantity;
    }
}
