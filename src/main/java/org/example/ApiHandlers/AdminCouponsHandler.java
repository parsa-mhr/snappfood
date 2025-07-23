
package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.Details.Coupon;
import org.example.Models.CouponRequestDto;
import org.example.Models.CouponResponseDto;
import org.example.Services.LocalDateTimeAdapter;
import org.example.User.User;
import org.example.Validation.TokenUserValidator;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import static org.example.ApiHandlers.HttpUtils.*;

public class AdminCouponsHandler implements HttpHandler {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
    private static final SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        try {
            String token = exchange.getRequestHeaders().getFirst("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                sendError(exchange, 401, "Missing or invalid Authorization header");
                return;
            }
            token = token.replace("Bearer ", "");
            TokenUserValidator validator = new TokenUserValidator(sessionFactory);
            User user = validator.validate(token);
            if (user == null /* || !user.getRole().equals(UserRole.ADMIN)*/) {
                sendError(exchange, 401, "Unauthorized: Admin role required");
                return;
            }

            if (path.matches("/admin/coupons/?")) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetCoupons(exchange);
                } else if ("POST".equalsIgnoreCase(method)) {
                    handlePostCoupon(exchange);
                } else {
                    sendEmpty(exchange, 405);
                }
            } else if (path.matches("/admin/coupons/\\d+/?")) {
                Long couponId = Long.parseLong(path.replaceAll("/admin/coupons/(\\d+)/?", "$1"));
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetCouponById(exchange, couponId);
                } else if ("PUT".equalsIgnoreCase(method)) {
                    handlePutCoupon(exchange, couponId);
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    handleDeleteCoupon(exchange, couponId);
                } else {
                    sendEmpty(exchange, 405);
                }
            } else {
                sendError(exchange, 404, "Not found");
            }
        } catch (Exception e) {
            sendError(exchange, 400, "Error processing request: " + e.getMessage());
        }
    }

    private void handleGetCoupons(HttpExchange exchange) throws IOException {
        EntityManager em = sessionFactory.createEntityManager();
        try {
            List<Coupon> coupons = em.createQuery("SELECT c FROM Coupon c", Coupon.class)
                    .getResultList();
            List<CouponResponseDto> responseDtos = coupons.stream()
                    .map(CouponResponseDto::new)
                    .collect(Collectors.toList());
            sendJson(exchange, 200, responseDtos);
        } finally {
            em.close();
        }
    }

    private void handlePostCoupon(HttpExchange exchange) throws IOException {
        EntityManager em = sessionFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            CouponRequestDto request = gson.fromJson(new InputStreamReader(exchange.getRequestBody()), CouponRequestDto.class);
            if (request == null || request.getCode() == null || request.getDiscount() == null || request.getDiscountType() == null || request.getMin_price() == null) {
                sendError(exchange, 400, "Invalid payload: code, discount, and discountType are required");
                return;
            }
            if (!request.getDiscountType().equals("percentage") && !request.getDiscountType().equals("fixed")) {
                sendError(exchange, 400, "Invalid discountType: must be 'percentage' or 'fixed'");
                return;
            }
            if (request.getDiscount().compareTo(BigDecimal.ZERO) <= 0) {
                sendError(exchange, 400, "Discount must be greater than 0");
                return;
            }
            Coupon coupon = new Coupon();
            coupon.setCode(request.getCode());
            coupon.setDiscount(request.getDiscount());
            coupon.setDiscountType(request.getDiscountType());
            coupon.setMin_price(request.getMin_price());
            coupon.setMaxUses(request.getMaxUses() != null ? request.getMaxUses() : 0);
            coupon.setUsedCount(0);
            if (request.getEnd_date() != null) {
                coupon.setExpirationDate(LocalDate.parse(request.getEnd_date(), DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay());            }
            coupon.setStatus("active");

            tx.begin();
            em.persist(coupon);
            tx.commit();

            CouponResponseDto responseDto = new CouponResponseDto(coupon);
            sendJson(exchange, 201, responseDto);
        } catch (JsonSyntaxException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            sendError(exchange, 400, "Failed to create coupon: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    private void handleGetCouponById(HttpExchange exchange, Long couponId) throws IOException {
        EntityManager em = sessionFactory.createEntityManager();
        try {
            Coupon coupon = em.find(Coupon.class, couponId);
            if (coupon == null) {
                sendError(exchange, 404, "Coupon not found: " + couponId);
                return;
            }
            CouponResponseDto responseDto = new CouponResponseDto(coupon);
            sendJson(exchange, 200, responseDto);
        } finally {
            em.close();
        }
    }

    private void handlePutCoupon(HttpExchange exchange, Long couponId) throws IOException {
        EntityManager em = sessionFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            Coupon coupon = em.find(Coupon.class, couponId);
            if (coupon == null) {
                sendError(exchange, 404, "Coupon not found: " + couponId);
                return;
            }
            CouponRequestDto request = gson.fromJson(new InputStreamReader(exchange.getRequestBody()), CouponRequestDto.class);
            if (request == null) {
                sendError(exchange, 400, "Invalid payload");
                return;
            }
            if (request.getCode() != null) {
                coupon.setCode(request.getCode());
            }
            if (request.getDiscount() != null) {
                if (request.getDiscount().compareTo(BigDecimal.ZERO) <= 0) {
                    sendError(exchange, 400, "Discount must be greater than 0");
                    return;
                }
                coupon.setDiscount(request.getDiscount());
            }
            if (request.getDiscountType() != null) {
                if (!request.getDiscountType().equals("percentage") && !request.getDiscountType().equals("fixed")) {
                    sendError(exchange, 400, "Invalid discountType: must be 'percentage' or 'fixed'");
                    return;
                }
                coupon.setDiscountType(request.getDiscountType());
            }
            if (request.getMaxUses() != null) {
                coupon.setMaxUses(request.getMaxUses());
            }
            if (request.getEnd_date() != null) {
                coupon.setExpirationDate(LocalDateTime.parse(request.getEnd_date(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            if (request.getMin_price() != null)
                coupon.setMin_price(request.getMin_price());

            tx.begin();
            em.merge(coupon);
            tx.commit();

            CouponResponseDto responseDto = new CouponResponseDto(coupon);
            sendJson(exchange, 200, responseDto);
        } catch (JsonSyntaxException e) {
            sendError(exchange, 400, "Invalid JSON payload");
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            sendError(exchange, 400, "Failed to update coupon: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    private void handleDeleteCoupon(HttpExchange exchange, Long couponId) throws IOException {
        EntityManager em = sessionFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            Coupon coupon = em.find(Coupon.class, couponId);
            if (coupon == null) {
                sendError(exchange, 404, "Coupon not found: " + couponId);
                return;
            }
            tx.begin();
            em.remove(coupon);
            tx.commit();
            sendEmpty(exchange, 204);
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            sendError(exchange, 400, "Failed to delete coupon: " + e.getMessage());
        } finally {
            em.close();
        }
    }

}