package org.example.Services;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import org.example.Details.Cart;
import org.example.Details.CartItem;
import org.example.Details.OrderStatus;
import org.example.Models.OrderResponseDto;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
//import javax.persistence.criteria.CriteriaBuilder;
//import javax.persistence.criteria.CriteriaQuery;
//import javax.persistence.criteria.Predicate;
//import javax.persistence.criteria.Root;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DeliveryService {
    private static final SessionFactory sessionFactory = new Configuration()
            .configure("hibernate.cfg.xml")
            .addAnnotatedClass(Cart.class)
            .buildSessionFactory();

    // سفارش‌هایی که در مرحله پیدا کردن پیک هستند
    public List<OrderResponseDto> getAvailableDeliveries() {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Cart> cq = cb.createQuery(Cart.class);
            Root<Cart> root = cq.from(Cart.class);
            cq.where(cb.equal(root.get("status"), OrderStatus.FINDING_COURIER));
            List<Cart> carts = session.createQuery(cq).list();
            return carts.stream().map(this::toDto).collect(Collectors.toList());
        }
    }

    // تاریخچه سفارش‌ها
    public List<OrderResponseDto> getDeliveryHistory(String query) {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Cart> cq = cb.createQuery(Cart.class);
            Root<Cart> root = cq.from(Cart.class);
            // فیلترها در parseQuery داخلی قابل توسعه هستند
            List<Cart> carts = session.createQuery(cq).list();
            return carts.stream().map(this::toDto).collect(Collectors.toList());
        }
    }

    // تغییر وضعیت مستقیم (PATCH)
    public OrderResponseDto updateDeliveryStatus(String orderId, String newStatus) {
        Transaction tx = null;
        Long id = Long.valueOf(orderId);
        if (newStatus.equals("ON_THE_WAY") || newStatus.equals("COMPLETED")) {
            try (Session session = sessionFactory.openSession()) {
                tx = session.beginTransaction();
                Cart cart = session.get(Cart.class, id);
                if (cart == null) throw new RuntimeException("Order not found: " + id);
                cart.setStatus(OrderStatus.valueOf(newStatus));
                session.update(cart);
                tx.commit();
                return toDto(cart);
            } catch (Exception e) {
                if (tx != null) tx.rollback();
                throw e;
            }
        }else
            throw new RuntimeException("status is not valid");
    }

    // پیک قبول می‌کند
    public OrderResponseDto acceptDelivery(Long cartId, String courierId) {
        Transaction tx = null;
        try (Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            Cart cart = session.get(Cart.class, cartId);
            if (cart == null) throw new RuntimeException("Order not found: " + cartId);
            if (cart.getStatus() != OrderStatus.FINDING_COURIER)
                throw new RuntimeException("Invalid state for acceptance");
            cart.setCourier_Id(courierId);
            cart.setStatus(OrderStatus.ON_THE_WAY);
            session.update(cart);
            tx.commit();
            return toDto(cart);
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    // پیک تحویل می‌دهد
    public OrderResponseDto completeDelivery(Long cartId) {
        Transaction tx = null;
        try (Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            Cart cart = session.get(Cart.class, cartId);
            if (cart == null) throw new RuntimeException("Order not found: " + cartId);
            if (cart.getStatus() != OrderStatus.ON_THE_WAY)
                throw new RuntimeException("Invalid state for completion");
            cart.setStatus(OrderStatus.COMPLETED);
            session.update(cart);
            tx.commit();
            return toDto(cart);
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public OrderResponseDto toDto(Cart cart) {
        OrderResponseDto res = new OrderResponseDto();
        res.id = cart.getCart_id();
        res.delivery_address = cart.getDelivery_address();
        res.customer_id = cart.getBuyer().getId();
        res.vendor_id = cart.getRestaurant().getId();
        res.coupon_id = cart.getCoupon() != null ? cart.getCoupon().getId() : null;

        // فرض: در CartItem قیمت واحد آیتم هست
        Map<Long , Integer> itemIds = new HashMap<>();
        double rawPrice = 0;
        for (CartItem item : cart.getItems()) {
            itemIds.put(item.getMenuItem().getId() ,  item.getQuantity());
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
