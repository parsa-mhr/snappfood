package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.Restaurant.MenuCategory;
import org.example.Restaurant.Restaurant;
import org.example.User.Seller;
import org.example.User.User;
import org.example.User.UserRole;
import org.example.Validation.TokenUserValidator;
import org.example.Unauthorized.UnauthorizedException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.example.ApiHandlers.SendJson.jsonError;
import static org.example.ApiHandlers.SendJson.sendJson;

/**
 * کلاس RestaurantMenuApiHandler برای مدیریت درخواست‌های POST به endpoint /restaurants/{id}/menu
 * این endpoint برای افزودن دسته‌بندی جدید به منوی رستوران با شناسه مشخص استفاده می‌شود
 */
public class RestaurantMenuApiHandler implements HttpHandler {
    private final SessionFactory sessionFactory;
    private final Gson gson;

    /**
     * سازنده کلاس که SessionFactory را دریافت می‌کند
     * @param sessionFactory فکتوری برای ارتباط با پایگاه داده
     */
    public RestaurantMenuApiHandler(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.gson = new Gson();
    }

    /**
     * کلاس داخلی برای نگاشت بدنه درخواست JSON
     */
    private static class MenuCategoryRequest {
        @SerializedName("title")
        private String title;
    }

    /**
     * متد اصلی برای مدیریت درخواست‌های HTTP
     * @param exchange شیء HttpExchange برای مدیریت درخواست و پاسخ
     */
    @Override
    public void handle(HttpExchange exchange) {
        try {
            // لاگ برای عیب‌یابی
            System.out.println("Request Method: " + exchange.getRequestMethod() + ", Path: " + exchange.getRequestURI().getPath());

            // بررسی اینکه متد درخواست POST باشد
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJson(exchange, 405, jsonError("فقط متد POST مجاز است"));
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
            if (segments.length != 4 || !segments[3].equals("menu")) {
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
            StringBuilder requestBody = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    requestBody.append(line);
                }
            } catch (IOException e) {
                sendJson(exchange, 400, jsonError("خطا در خواندن بدنه درخواست: " + e.getMessage()));
                return;
            }

            // تبدیل JSON به شیء MenuCategoryRequest
            MenuCategoryRequest menuRequest;
            try {
                menuRequest = gson.fromJson(requestBody.toString(), MenuCategoryRequest.class);
            } catch (JsonSyntaxException e) {
                sendJson(exchange, 400, jsonError("فرمت JSON نامعتبر است: " + e.getMessage()));
                return;
            }

            // اعتبارسنجی فیلد title
            if (menuRequest == null || menuRequest.title == null || menuRequest.title.trim().isEmpty()) {
                sendJson(exchange, 400, jsonError("عنوان دسته‌بندی منو الزامی است"));
                return;
            }

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
                MenuCategory menuCategory = new MenuCategory(menuRequest.title, restaurant);

                // ذخیره دسته‌بندی در پایگاه داده
                try {
                    session.beginTransaction();
                    session.persist(menuCategory);
                    session.getTransaction().commit();
                } catch (Exception e) {
                    session.getTransaction().rollback();
                    e.printStackTrace();
                    sendJson(exchange, 500, jsonError("خطای داخلی سرور هنگام افزودن دسته‌بندی منو: " + e.getMessage()));
                    return;
                }

                // به‌روزرسانی منوی رستوران
                restaurant.loadMenu(session);

                // ارسال پاسخ موفقیت‌آمیز
                sendJson(exchange, 201, gson.toJson(Map.of(
                        "message", "دسته‌بندی با موفقیت به منوی رستوران اضافه شد",
                        "categoryId", menuCategory.getId())));
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, jsonError("خطای داخلی سرور: " + e.getMessage()));
        }
    }
}
