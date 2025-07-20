package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.Restaurant.MenuCategory;
import org.example.Restaurant.MenuItem;
import org.example.Restaurant.Restaurant;
import org.example.User.Seller;
import org.example.User.User;
import org.example.User.UserRole;
import org.example.Validation.TokenUserValidator;
import org.example.Unauthorized.UnauthorizedException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.example.ApiHandlers.SendJson.jsonError;
import static org.example.ApiHandlers.SendJson.sendJson;

/**
 * کلاس RestaurantsItemApiHandler برای مدیریت درخواست‌های POST به endpoint /restaurants/{id}/item
 * این endpoint برای افزودن آیتم جدید به منوی رستوران استفاده می‌شود
 */
public class RestaurantsItemApiHandler implements HttpHandler {
    private final SessionFactory sessionFactory;
    private final Gson gson;

    public RestaurantsItemApiHandler(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            // لاگ متد و مسیر درخواست برای عیب‌یابی
            System.out.println("Request Method: " + exchange.getRequestMethod() + ", Path: " + exchange.getRequestURI().getPath());

            // بررسی اینکه متد درخواست POST باشد
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJson(exchange, 405, jsonError("متد مجاز نیست"));
                return;
            }

            // بررسی نوع محتوای درخواست
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.contains("application/json")) {
                sendJson(exchange, 415, jsonError("نوع رسانه پشتیبانی‌نشده است"));
                return;
            }

            // استخراج هدر Authorization
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                sendJson(exchange, 401, jsonError("هدر Authorization نامعتبر است یا فرمت Bearer مورد انتظار نیست"));
                return;
            }

            // استخراج توکن از هدر
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

            // بررسی نقش کاربر (فقط فروشنده مجاز است)
            if (user.getRole() == null || user.getRole() != UserRole.seller) {
                sendJson(exchange, 403, jsonError("فقط فروشندگان مجاز به افزودن آیتم به منو هستند"));
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
            if (segments.length != 4 || !segments[3].equals("item")) {
                sendJson(exchange, 400, jsonError("شناسه رستوران یا مسیر نامعتبر است"));
                return;
            }
            Long restaurantId;
            try {
                restaurantId = Long.parseLong(segments[2]);
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, jsonError("شناسه رستوران باید عدد باشد"));
                return;
            }

            // خواندن داده‌های JSON از بدنه درخواست
            Map<String, Object> body;
            try {
                body = gson.fromJson(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8),
                        new TypeToken<Map<String, Object>>() {}.getType());
            } catch (Exception e) {
                sendJson(exchange, 400, jsonError("بدنه درخواست نامعتبر است"));
                return;
            }

            // بررسی فیلدهای اجباری بر اساس اسکیما
            if (!body.containsKey("name") || !body.containsKey("description") || !body.containsKey("price") ||
                !body.containsKey("supply") || !body.containsKey("keywords")) {
                sendJson(exchange, 400, jsonError("فیلدهای اجباری name، description، price، supply یا keywords وجود ندارند"));
                return;
            }

            // اعتبارسنجی فیلدها
            String name = body.get("name").toString().trim();
            if (name.isEmpty()) {
                sendJson(exchange, 400, jsonError("نام آیتم نمی‌تواند خالی باشد"));
                return;
            }
            String description = body.get("description").toString().trim();
            if (description.isEmpty()) {
                sendJson(exchange, 400, jsonError("توضیحات آیتم نمی‌تواند خالی باشد"));
                return;
            }
            int price;
            int supply;
            try {
                price = ((Number) body.get("price")).intValue();
                supply = ((Number) body.get("supply")).intValue();
                if (price < 0) {
                    sendJson(exchange, 400, jsonError("قیمت نمی‌تواند منفی باشد"));
                    return;
                }
                if (supply < 0) {
                    sendJson(exchange, 400, jsonError("موجودی نمی‌تواند منفی باشد"));
                    return;
                }
            } catch (ClassCastException | NullPointerException e) {
                sendJson(exchange, 400, jsonError("مقدار price یا supply نامعتبر است"));
                return;
            }
            List<String> keywords;
            try {
                keywords = (List<String>) body.get("keywords");
                if (keywords == null || keywords.isEmpty()) {
                    sendJson(exchange, 400, jsonError("لیست keywords نمی‌تواند خالی باشد"));
                    return;
                }
                for (String keyword : keywords) {
                    if (keyword == null || keyword.trim().isEmpty()) {
                        sendJson(exchange, 400, jsonError("کلمات کلیدی نمی‌توانند خالی باشند"));
                        return;
                    }
                }
            } catch (ClassCastException e) {
                sendJson(exchange, 400, jsonError("فرمت keywords نامعتبر است"));
                return;
            }

            // بررسی وجود menu_title (اختیاری برای MenuCategory)
            String menuTitle = body.containsKey("menu_title") ? body.get("menu_title").toString().trim() : null;

            // دریافت رستوران و بررسی مالکیت
            try (Session session = sessionFactory.openSession()) {
                Restaurant restaurant = session.get(Restaurant.class, restaurantId);
                if (restaurant == null) {
                    sendJson(exchange, 404, jsonError("رستوران یافت نشد"));
                    return;
                }
                if (!restaurant.getSeller().getId().equals(user.getId())) {
                    sendJson(exchange, 403, jsonError("شما مجاز به افزودن آیتم به این رستوران نیستید"));
                    return;
                }

                // بررسی وجود MenuCategory (اگر menu_title ارائه شده باشد)
                MenuCategory menuCategory = null;
                if (menuTitle != null && !menuTitle.isEmpty()) {
                    menuCategory = session.createQuery("FROM MenuCategory mc WHERE mc.restaurant.id = :restaurantId AND mc.title = :title", MenuCategory.class)
                            .setParameter("restaurantId", restaurantId)
                            .setParameter("title", menuTitle)
                            .uniqueResult();
                    if (menuCategory == null) {
                        sendJson(exchange, 400, jsonError("دسته‌بندی منو با عنوان " + menuTitle + " یافت نشد"));
                        return;
                    }
                }

                // ساخت آیتم منو
                MenuItem item = new MenuItem();
                item.setName(name);
                item.setDescription(description);
                item.setPrice(price);
                item.setSupply(supply);
                item.setKeywords(keywords);
                item.setRestaurant(restaurant);
                if (body.containsKey("image")) {
                    String image = body.get("image").toString().trim();
                    if (!image.isEmpty()) {
                        try {
                            item.setImageBase64(image);
                        } catch (IllegalArgumentException e) {
                            sendJson(exchange, 400, jsonError("فرمت image نامعتبر است"));
                            return;
                        }
                    }
                }

                // ذخیره آیتم در پایگاه داده
                try {
                    session.beginTransaction();
                    session.persist(item);
                    if (menuCategory != null) {
                        menuCategory.addItem(item);
                        session.merge(menuCategory);
                    }
                    session.getTransaction().commit();
                } catch (Exception e) {
                    session.getTransaction().rollback();
                    e.printStackTrace();
                    sendJson(exchange, 500, jsonError("خطای داخلی سرور هنگام افزودن آیتم: " + e.getMessage()));
                    return;
                }

                // به‌روزرسانی منوی رستوران
                restaurant.loadMenu(session);

                // ارسال پاسخ با اسکیما food_item
                Map<String, Object> response = Map.of(
                        "id", item.getId(),
                        "name", item.getName(),
                        "description", item.getDescription(),
                        "price", item.getPrice(),
                        "supply", item.getSupply(),
                        "keywords", item.getKeywords(),
                        "image", item.getImage() != null ? item.getImage() : ""
                );
                sendJson(exchange, 201, gson.toJson(response));
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, jsonError("خطای داخلی سرور: " + e.getMessage()));
        }
    }
}
