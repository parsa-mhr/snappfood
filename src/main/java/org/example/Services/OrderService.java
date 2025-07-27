package org.example.Services;

import org.example.DAO.OrderDAO;
import org.example.Details.*;
import org.example.Models.CreateOrderReq;
import org.example.Models.HistoryBody;
import org.example.Models.OrderResponseDto;
import org.example.Restaurant.MenuItem;
import org.example.Restaurant.Restaurant;
import org.example.User.Buyer;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Order;
import org.hibernate.query.Query;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class OrderService {
    private final SessionFactory factory = new Configuration().configure().buildSessionFactory();
    OrderDAO orderDAO = new OrderDAO(factory);
    public OrderDTO createOrder(CreateOrderReq request, Buyer buyer) {
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
            cart.setStatus(OrderStatus.SUBMITTED);
            long rawPrice = 0;

            for (CreateOrderReq.ItemRequest itemRequest : request.items) {
                MenuItem menuItem = session.get(MenuItem.class, itemRequest.item_id);
                if (menuItem == null) throw new IllegalArgumentException("Invalid item id: " + itemRequest.item_id);

                CartItem cartItem = new CartItem(cart, menuItem, itemRequest.quantity);
                cart.getItems().add(cartItem);
                rawPrice += (long) cartItem.getMenuItem().getPrice() * cartItem.getQuantity();
            }
            cart.setPay_price((long) (rawPrice * 1.09));

            session.save(cart);
            session.getTransaction().commit();
            OrderDTO response = toResponse(cart);

            return response;
        }
    }


    public OrderDTO getById(int id) {
        try (Session s = factory.openSession()) {
        Optional <Cart> cart = Optional.ofNullable(s.get(Cart.class, id));
        if (cart.isEmpty()) return null ;
        else
            return toResponse(cart.get());
        }
    }
    public Cart getCartById(int id) {
        try (Session s = factory.openSession()) {
            return s.createQuery(
                            "SELECT c FROM Cart c JOIN FETCH c.items i JOIN FETCH i.menuItem WHERE c.id = :id"
                            , Cart.class)
                    .setParameter("id", id)
                    .uniqueResult();
        }
    }


    public List<OrderDTO> getHistory(int buyerId) {
        try (Session s = factory.openSession()) {
            // توجه کنید که اینجا o.buyer.id را با پارامتر bid مقایسه می‌کنیم
            Query<Cart> q = s.createQuery(
                    "FROM Cart o WHERE o.buyer.id = :bid AND o.status IN (:statuses) ORDER BY o.id DESC" ,
                    Cart.class
            );
            q.setParameter("bid", buyerId);
            q.setParameter("statuses", Arrays.asList(OrderStatus.COMPLETED, OrderStatus.ACCEPTED , OrderStatus.FINDING_COURIER , OrderStatus.ON_THE_WAY , OrderStatus.WAITING_VENDOR , OrderStatus.SUBMITTED));

            List<Cart> hist = q.list();
            for (Cart cart : hist) {
                Hibernate.initialize(cart.getBuyer());
                Hibernate.initialize(cart.getRestaurant());
                Hibernate.initialize(cart.getCoupon());
                Hibernate.initialize(cart.getItems());
            }
            List <OrderDTO> dtos = new ArrayList<>();
            for (Cart cart : hist) {
                dtos.add(toResponse(cart));
            }
            return dtos;
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
                q.setParameter("statuses",  Arrays.asList(OrderStatus.COMPLETED, OrderStatus.ACCEPTED , OrderStatus.FINDING_COURIER , OrderStatus.ON_THE_WAY , OrderStatus.WAITING_VENDOR));
                q.setParameter("searchText", body.getSearch()); // ممکنه null باشه
                q.setParameter("restaurantName", body.getVendor()); // ممکنه null باشه

                return q.list();
            }

    }
        public static OrderDTO toResponse(Cart cart) {
            OrderDTO res = new OrderDTO(
                    cart.getCart_id(),
                    cart.getDelivery_address(),
                    cart.getBuyer().getId(),
                    cart.getBuyer().getFullName() ,
                    cart.getRestaurant().getId(),
                    cart.getCoupon() != null ? cart.getCoupon().getId() : null,
                    cart.getItems().stream()
                            .collect(Collectors.toMap(
                                    item -> item.getMenuItem().getId(),
                                    CartItem::getQuantity
                            )) ,
                    cart.getPay_price() ,
                    cart.getRestaurant().getTaxFee(),
                    cart.getRestaurant().getAdditionalFee(),
                    0 ,
                    cart.getPay_price(),
                    cart.getCourier_Id() != null ? Long.valueOf(cart.getCourier_Id()) : null,
                    cart.getStatus() != null ? cart.getStatus().toString() : "UNKNOWN",
                    cart.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    cart.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );

            // فرض: در CartItem قیمت واحد آیتم هست
            return res;
        }


    public List<OrderDTO> findAll() {
        System.out.println("OrderService.findAll called"); // Debug log
        List<Cart> orders = orderDAO.findAll();
        System.out.println("Carts fetched: " + orders.size()); // Debug log
        List<OrderDTO> list = new ArrayList<>();
        for (Cart cart : orders) {
            list.add(toResponse(cart));
        }
        return list;
    }
}
