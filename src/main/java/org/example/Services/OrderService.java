package org.example.Services;

import org.example.Details.Cart;
import org.example.Details.CartItem;
import org.example.Details.OrderStatus;
import org.example.Models.Coupon;
import org.example.Models.CreateOrderReq;
import org.example.Models.HistoryBody;
import org.example.Models.OrderResponseDto;
import org.example.Restaurant.MenuItem;
import org.example.Restaurant.Restaurant;
import org.example.User.Buyer;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Order;
import org.hibernate.query.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class OrderService {
    private final SessionFactory factory = new Configuration().configure().buildSessionFactory();

    public OrderResponseDto createOrder(CreateOrderReq request, Buyer buyer) {
        try (Session session = factory.openSession()) {
            session.beginTransaction();

            Restaurant restaurant = session.get(Restaurant.class, request.vendor_id);
            if (restaurant == null) throw new IllegalArgumentException("Invalid restaurant");

            Coupon coupon = null;
            if (request.coupon_id != null) {
                coupon = session.get(Coupon.class, request.coupon_id);
            }

            Cart cart = new Cart();
            cart.setBuyer(buyer);
            cart.setRestaurant(restaurant);
            cart.setCoupon(coupon);
            cart.setDelivery_address(request.delivery_address);
            cart.setStatus(OrderStatus.Pending);

            for (CreateOrderReq.ItemRequest itemRequest : request.items) {
                MenuItem menuItem = session.get(MenuItem.class, itemRequest.item_id);
                if (menuItem == null) throw new IllegalArgumentException("Invalid item id: " + itemRequest.item_id);

                CartItem cartItem = new CartItem(cart, menuItem, itemRequest.quantity);
                cart.getItems().add(cartItem);
            }

            session.save(cart);
            session.getTransaction().commit();
            OrderResponseDto response = toResponse(cart);

            return response;
        }
    }


    public OrderResponseDto getById(int id) {
        try (Session s = factory.openSession()) {
        Optional <Cart> cart = Optional.ofNullable(s.get(Cart.class, id));
        if (cart.isEmpty()) return null ;
        else
            return toResponse(cart.get());
        }
    }

    public List<Cart> getHistory(int buyerId) {
        try (Session s = factory.openSession()) {
            // توجه کنید که اینجا o.buyer.id را با پارامتر bid مقایسه می‌کنیم
            Query<Cart> q = s.createQuery(
                    "FROM Cart o WHERE o.buyer.id = :bid AND o.status IN (:statuses) ORDER BY o.id DESC" ,
                    Cart.class
            );
            q.setParameter("bid", buyerId);
            q.setParameter("statuses", Arrays.asList(OrderStatus.Deliverd, OrderStatus.Current_Order));

            return q.list();
        }
    }
    public List<Cart> SearchHistory(HistoryBody body , String buyerId) {

            try (Session s = factory.openSession()) {
                String hql = """
        SELECT DISTINCT c
        FROM Cart c
        JOIN c.items i
        JOIN i.menuItem m
        JOIN m.restaurant r
        WHERE c.buyer.id = :bid
        AND c.status IN (:statuses)
        AND (
            (:searchText IS NULL OR 
             LOWER(m.title) LIKE LOWER(CONCAT('%', :searchText, '%')) OR 
             LOWER(m.description) LIKE LOWER(CONCAT('%', :searchText, '%')))
        )
        AND (
            :restaurantName IS NULL OR 
            LOWER(r.name) LIKE LOWER(CONCAT('%', :restaurantName, '%'))
        )
        ORDER BY c.cart_id DESC
    """;

                Query<Cart> q = s.createQuery(hql, Cart.class);
                q.setParameter("bid", buyerId);
                q.setParameter("statuses", Arrays.asList(OrderStatus.Deliverd, OrderStatus.Current_Order));
                q.setParameter("searchText", body.getSearch()); // ممکنه null باشه
                q.setParameter("restaurantName", body.getVendor()); // ممکنه null باشه

                return q.list();
            }

    }
        public static OrderResponseDto toResponse(Cart cart) {
            OrderResponseDto res = new OrderResponseDto();
            res.id = cart.getCart_id();
            res.delivery_address = cart.getDelivery_address();
            res.customer_id = cart.getBuyer().getId();
            res.vendor_id = cart.getRestaurant().getId();
            res.coupon_id = cart.getCoupon() != null ? cart.getCoupon().getId() : null;

            // فرض: در CartItem قیمت واحد آیتم هست
            List<Long> itemIds = new ArrayList<>();
            double rawPrice = 0;
            for (CartItem item : cart.getItems()) {
                itemIds.add(item.getMenuItem().getId());
                rawPrice += item.getMenuItem().getPrice() * item.getQuantity();
            }

            res.item_ids = itemIds;
            res.raw_price = rawPrice;
            res.tax_fee = rawPrice * 0.09; // فرض: 9٪ مالیات
            res.additional_fee = 0;
            res.courier_fee = 30000; // عدد فرضی
            res.pay_price = res.raw_price + res.tax_fee + res.additional_fee + res.courier_fee;
            cart.setPay_price((long) res.pay_price);
            res.courier_id = null; // در لحظه سفارش‌گذاری تعیین نمی‌شود
            res.status = cart.getStatus().toString();

            res.created_at = cart.getCreatedAt().toString();
            res.updated_at = cart.getUpdatedAt().toString();

            return res;
        }


}
