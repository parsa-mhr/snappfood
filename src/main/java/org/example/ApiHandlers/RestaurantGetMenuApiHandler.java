package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.Restaurant.MenuCategory;
import org.example.Restaurant.MenuCategoryDTO;
import org.example.Restaurant.Restaurant;
import org.example.Unauthorized.UnauthorizedException;
import org.example.User.Seller;
import org.example.User.User;
import org.example.User.UserRole;
import org.example.Validation.TokenUserValidator;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.StreamSupport.stream;
import static org.example.ApiHandlers.SendJson.jsonError;
import static org.example.ApiHandlers.SendJson.sendJson;

public class RestaurantGetMenuApiHandler implements HttpHandler {
    private final SessionFactory sessionFactory;
    private final Gson gson;

    /**
     * سازنده کلاس که SessionFactory را دریافت می‌کند
     *
     * @param sessionFactory فکتوری برای ارتباط با پایگاه داده
     */
    public RestaurantGetMenuApiHandler(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            // لاگ برای عیب‌یابی
            System.out.println("Request Method: " + exchange.getRequestMethod() + ", Path: " + exchange.getRequestURI().getPath());

            // بررسی اینکه متد درخواست POST باشد
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendJson(exchange, 405, jsonError("فقط متد GET مجاز است"));
                return;
            }

            // استخراج هدر Authorization
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                sendJson(exchange, 401, jsonError("هدر Authorization نامعتبر است یا فرمت Bearer مورد انتظار نیست"));
                return;
            }

            // استخراج توکن
            String token = authHeader.substring("Bearer ".length()).trim();
            if (token.isEmpty()) {
                sendJson(exchange, 401, jsonError("توکن ارائه نشده است"));
                return;
            }

            // اعتبارسنجی توکن و دریافت کاربر
            TokenUserValidator validator = new TokenUserValidator(sessionFactory);
            User user;
            try {
                user = validator.validate(token);
                if (user == null) {
                    sendJson(exchange, 401, jsonError("کاربر مرتبط با توکن یافت نشد"));
                    return;
                }
            } catch (UnauthorizedException e) {
                String message;
                switch (e.getErrorCode()) {
                    case "INVALID_TOKEN":
                        message = "توکن نامعتبر است";
                        break;
                    case "EXPIRED_TOKEN":
                        message = "توکن منقضی شده است";
                        break;
                    case "MISSING_USER":
                        message = "کاربر مرتبط با توکن یافت نشد";
                        break;
                    case "INACTIVE_USER":
                        message = "حساب کاربر غیرفعال است";
                        break;
                    default:
                        message = "خطای احراز هویت: " + e.getMessage();
                }
                sendJson(exchange, 401, jsonError(message));
                return;
            }

            // بررسی نقش کاربر
            if (user.getRole() == null || user.getRole() != UserRole.seller) {
                sendJson(exchange, 403, jsonError("فقط فروشندگان می‌توانند دسته‌بندی منو اضافه کنند"));
                return;
            }

            // بررسی نوع کاربر (Seller)
            if (!(user instanceof Seller)) {
                sendJson(exchange, 403, jsonError("کاربر باید از نوع فروشنده باشد"));
                return;
            }

            // استخراج شناسه رستوران از مسیر
            String path = exchange.getRequestURI().getPath();
            String[] segments = path.split("/");
            if (segments.length != 4 || !segments[3].equals("menus")) {
                sendJson(exchange, 400, jsonError("مسیر یا شناسه رستوران نامعتبر است"));
                return;
            }
            Long restaurantId;
            try {
                restaurantId = Long.parseLong(segments[2]);
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, jsonError("شناسه رستوران باید عدد باشد: " + e.getMessage()));
                return;
            }

            // دریافت بدنه درخواست (JSON)

            // دریافت رستوران و بررسی مالکیت
            try (Session session = sessionFactory.openSession()) {
                Restaurant restaurant = session.get(Restaurant.class, restaurantId);
                if (restaurant == null) {
                    sendJson(exchange, 404, jsonError("رستوران با شناسه " + restaurantId + " یافت نشد"));
                    return;
                }
                if (!restaurant.getSeller().getId().equals(user.getId())) {
                    sendJson(exchange, 403, jsonError("شما مجاز به افزودن دسته‌بندی به منوی این رستوران نیستید"));
                    return;
                }

                // ایجاد دسته‌بندی منو
                List<MenuCategory> menuCategory = session.createQuery(
                                "FROM MenuCategory m WHERE m.restaurant.id = :restaurantId",
                                MenuCategory.class)
                        .setParameter("restaurantId", restaurantId)
                        .list();

                List<MenuCategoryDTO> categoryDTOs = menuCategory.stream()
                        .map(MenuCategoryDTO::new)
                        .collect(Collectors.toList());


                // ارسال پاسخ موفقیت‌آمیز
                sendJson(exchange, 200, gson.toJson(menuCategory));
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, jsonError("خطای داخلی سرور: " + e.getMessage()));
        }
    }
}