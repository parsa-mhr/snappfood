package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.DAO.WalletDAO;
import org.example.Details.Cart;
import org.example.Details.OrderStatus;
import org.example.Models.Wallet;
import org.example.Restaurant.Restaurant;
import org.example.User.User;
import org.example.User.UserRole;
import org.example.Validation.TokenUserValidator;
import org.example.Unauthorized.UnauthorizedException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.example.ApiHandlers.SendJson.jsonError;
import static org.example.ApiHandlers.SendJson.sendJson;

/**
 * کلاس RestaurantOrderStatusUpdateApiHandler برای مدیریت درخواست‌های PATCH به endpoint /restaurants/orders/{order_id}
 * این endpoint برای تغییر وضعیت یک سفارش خاص در رستوران استفاده می‌شود
 */
public class RestaurantOrderStatusUpdateApiHandler implements HttpHandler {
    private final SessionFactory sessionFactory;
    private final Gson gson;
    private final WalletDAO walletDAO ;

    public RestaurantOrderStatusUpdateApiHandler(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.gson = new Gson();
        walletDAO = new WalletDAO(sessionFactory);
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            System.out.println("LOG: Request received for order status update. Method: " + exchange.getRequestMethod());

            // بررسی متد PATCH
            if (!exchange.getRequestMethod().equalsIgnoreCase("PATCH")) {
                System.out.println("LOG: Method not PATCH. Returned 405.");
                sendJson(exchange, 405, jsonError("فقط متد PATCH مجاز است"));
                return;
            }

            // بررسی نوع محتوای درخواست
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            System.out.println("LOG: Content-Type: " + contentType);
            if (contentType == null || !contentType.contains("application/json")) {
                System.out.println("LOG: Invalid Content-Type. Returned 415.");
                sendJson(exchange, 415, jsonError("نوع رسانه پشتیبانی‌نشده است"));
                return;
            }

            // استخراج هدر Authorization
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            System.out.println("LOG: Authorization Header: " + (authHeader != null ? authHeader.substring(0, Math.min(authHeader.length(), 30)) + "..." : "null")); // Print first 30 chars
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                System.out.println("LOG: Invalid Authorization Header. Returned 401.");
                sendJson(exchange, 401, jsonError("هدر Authorization نامعتبر است یا فرمت Bearer مورد انتظار نیست"));
                return;
            }

            // استخراج توکن
            String token = authHeader.substring("Bearer ".length()).trim();
            if (token.isEmpty()) {
                System.out.println("LOG: Empty token. Returned 401.");
                sendJson(exchange, 401, jsonError("توکن ارائه نشده است"));
                return;
            }
            System.out.println("LOG: Token extracted. Length: " + token.length());

            // اعتبارسنجی توکن و دریافت کاربر
            TokenUserValidator validator = new TokenUserValidator(sessionFactory);
            User user;
            try {
                user = validator.validate(token);
                if (user == null) {
                    System.out.println("LOG: User not found for token. Returned 401.");
                    sendJson(exchange, 401, jsonError("کاربر مرتبط با توکن یافت نشد"));
                    return;
                }
                System.out.println("LOG: Token validated. User ID: " + user.getId() + ", Role: " + user.getRole());
            } catch (UnauthorizedException e) {
                String message;
                switch (e.getErrorCode()) {
                    case "INVALID_TOKEN": message = "توکن نامعتبر است"; break;
                    case "EXPIRED_TOKEN": message = "توکن منقضی شده است"; break;
                    case "MISSING_USER": message = "کاربر مرتبط با توکن یافت نشد"; break;
                    case "INACTIVE_USER": message = "حساب کاربر غیرفعال است"; break;
                    default: message = "خطای احراز هویت: " + e.getMessage();
                }
                System.out.println("LOG: UnauthorizedException: " + e.getErrorCode() + ". Returned 401.");
                sendJson(exchange, 401, jsonError(message));
                return;
            }

            // بررسی نقش کاربر
            if (user.getRole() == null || user.getRole() != UserRole.seller) {
                System.out.println("LOG: User is not seller. Role: " + user.getRole() + ". Returned 403.");
                sendJson(exchange, 403, jsonError("فقط فروشندگان می‌توانند وضعیت سفارش را تغییر دهند"));
                return;
            }

            // استخراج شناسه سفارش از مسیر
            String path = exchange.getRequestURI().getPath();
            System.out.println("LOG: Request path: " + path);
            String[] segments = path.split("/");
            if (segments.length != 4 || !segments[2].equals("orders")) {
                System.out.println("LOG: Invalid path segments. Returned 400.");
                sendJson(exchange, 400, jsonError("مسیر یا شناسه سفارش نامعتبر است"));
                return;
            }
            Long orderId;
            try {
                orderId = Long.parseLong(segments[3]);
                System.out.println("LOG: Order ID extracted: " + orderId);
            } catch (NumberFormatException e) {
                System.out.println("LOG: Invalid Order ID format. Returned 400.");
                sendJson(exchange, 400, jsonError("شناسه سفارش باید عدد باشد: " + e.getMessage()));
                return;
            }

            // خواندن بدنه درخواست
            Map<String, String> body;
            try {
                body = gson.fromJson(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8),
                        new TypeToken<Map<String, String>>() {}.getType());
                System.out.println("LOG: Request body read. Contains status: " + body.containsKey("status"));
            } catch (Exception e) {
                System.out.println("LOG: Invalid request body JSON. Returned 400.");
                sendJson(exchange, 400, jsonError("بدنه درخواست نامعتبر است"));
                return;
            }

            // بررسی فیلد status
            if (!body.containsKey("status")) {
                System.out.println("LOG: 'status' field missing in body. Returned 400.");
                sendJson(exchange, 400, jsonError("فیلد status الزامی است"));
                return;
            }

            // اعتبارسنجی مقدار status
            OrderStatus newStatus;
            try {
                newStatus = OrderStatus.valueOf(body.get("status").toUpperCase());
                System.out.println("LOG: New status parsed: " + newStatus);
            } catch (IllegalArgumentException e) {
                System.out.println("LOG: Invalid status value in body: " + body.get("status") + ". Returned 400.");
                sendJson(exchange, 400, jsonError("وضعیت نامعتبر است: " + body.get("status")));
                return;
            }

            // دریافت و به‌روزرسانی سفارش
            try (Session session = sessionFactory.openSession()) {
                Cart order = session.get(Cart.class, orderId);
                if (order == null) {
                    System.out.println("LOG: Order not found in DB for ID: " + orderId + ". Returned 404.");
                    sendJson(exchange, 404, jsonError("سفارش با شناسه " + orderId + " یافت نشد"));
                    return;
                }
                System.out.println("LOG: Order found in DB. Current Status: " + order.getStatus());

                // بررسی مالکیت رستوران
                Restaurant restaurant = order.getRestaurant();
                if (restaurant == null || !restaurant.getSeller().getId().equals(user.getId())) {
                    System.out.println("LOG: User does not own this restaurant/order. Returned 403.");
                    sendJson(exchange, 403, jsonError("شما مجاز به تغییر وضعیت این سفارش نیستید"));
                    return;
                }
                System.out.println("LOG: Ownership verified.");

                // **اعتبارسنجی جدید چرخه عمر سفارش**
                OrderStatus currentStatus = order.getStatus();
                System.out.println("LOG: Calling isValidStatusTransition. Current: " + currentStatus + ", New: " + newStatus);
                if (!isValidStatusTransition(currentStatus, newStatus)) {
                    System.out.println("LOG: Invalid status transition: " + currentStatus + " to " + newStatus + ". Returned 400.");
                    sendJson(exchange, 400, jsonError("تغییر وضعیت از " + currentStatus + " به " + newStatus + " مجاز نیست."));
                    return;
                }
                System.out.println("LOG: Status transition is VALID.");

                Transaction transaction = session.beginTransaction();
                try {
                    // منطق بازپرداخت (بدون تغییر)
                    if (newStatus.equals(OrderStatus.CANCELLED) && !currentStatus.equals(OrderStatus.CANCELLED)) {
                        Wallet wallet = walletDAO.findByUserId(order.getBuyer().getId());
                        if (wallet == null) {
                            wallet = new Wallet();
                            wallet.setId(order.getBuyer().getId());
                            wallet.setBalance(BigDecimal.valueOf(0));
                            session.save(wallet);
                        }
                        wallet.setBalance(wallet.getBalance().add(BigDecimal.valueOf(order.getPay_price())));
                        session.update(wallet);

                    }
                    if (newStatus.equals(OrderStatus.CANCELLED) && !currentStatus.equals(OrderStatus.CANCELLED)) {
                        Wallet wallet = walletDAO.findByUserId(order.getBuyer().getId());
                        if (wallet == null) {
                            wallet = new Wallet();
                            wallet.setId(order.getBuyer().getId());
                            wallet.setBalance(BigDecimal.valueOf(0));
                            session.save(wallet);
                        }
                        wallet.setBalance(wallet.getBalance().add(BigDecimal.valueOf(order.getPay_price())));
                        session.update(wallet);
                    }

                    order.setStatus(newStatus);
                    order.setUpdatedAt(LocalDateTime.now());
                    session.update(order);
                    transaction.commit();
                    System.out.println("LOG: Order status updated successfully in DB.");
                } catch (Exception e) {
                    transaction.rollback();
                    System.out.println("LOG: Transaction rolled back. Error: " + e.getMessage());
                    sendJson(exchange, 500, jsonError("خطا در به‌روزرسانی وضعیت سفارش: " + e.getMessage()));
                    return;
                }

                // ارسال پاسخ موفقیت‌آمیز
                System.out.println("LOG: Sending success response (200 OK).");
                sendJson(exchange, 200, gson.toJson(Map.of(
                        "message", "وضعیت سفارش با موفقیت به " + newStatus + " تغییر کرد",
                        "order_id", orderId)));
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("LOG: Unhandled exception in handle method: " + e.getMessage());
            sendJson(exchange, 500, jsonError("خطای داخلی سرور: " + e.getMessage()));
        }
    }

    private boolean isValidStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        System.out.println("LOG: Entering isValidStatusTransition. Current: " + currentStatus + ", New: " + newStatus);
        boolean isValid;
        switch (currentStatus) {
            case SUBMITTED:
                isValid = newStatus == OrderStatus.WAITING_RESTAURANT;
                break;
            case WAITING_RESTAURANT:
                isValid = newStatus == OrderStatus.ACCEPTED || newStatus == OrderStatus.CANCELLED;
                break;
            case ACCEPTED:
                isValid = newStatus == OrderStatus.FINDING_COURIER;
                break;
            case FINDING_COURIER:
                isValid = newStatus == OrderStatus.COURIER_ACCEPTED;
                break;
            case COURIER_ACCEPTED:
                isValid = newStatus == OrderStatus.ON_THE_WAY;
                break;
            case ON_THE_WAY:
                isValid = newStatus == OrderStatus.COMPLETED;
                break;
            case COMPLETED:
            case CANCELLED:
                isValid = false;
                break;
            case WAITING_VENDOR:
                isValid = false; // هیچ اقدام مستقیم فروشنده از اینجا.
                break;
            default:
                isValid = false;
                break;
        }
        System.out.println("LOG: isValidStatusTransition result: " + isValid);
        return isValid;
    }
}