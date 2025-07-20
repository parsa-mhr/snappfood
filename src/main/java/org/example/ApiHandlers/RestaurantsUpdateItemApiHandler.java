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
import org.hibernate.exception.ConstraintViolationException;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.example.ApiHandlers.SendJson.jsonError;
import static org.example.ApiHandlers.SendJson.sendJson;

/**
 * کلاس RestaurantsUpdateItemApiHandler برای مدیریت درخواست‌های PUT به endpoint /restaurants/{id}/item/{item_id}
 * این endpoint برای به‌روزرسانی آیتم موجود در منوی رستوران استفاده می‌شود
 */
public class RestaurantsUpdateItemApiHandler implements HttpHandler {
    private final SessionFactory sessionFactory;
    private final Gson gson;

    public RestaurantsUpdateItemApiHandler(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            // لاگ برای عیب‌یابی
            System.out.println("Request Method: " + exchange.getRequestMethod() + ", Path: " + exchange.getRequestURI().getPath());

            // بررسی متد PUT
            if (!exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
                sendJson(exchange, 405, jsonError("متد مجاز نیست"));
                return;
            }

            // بررسی نوع محتوای درخواست
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.contains("application/json")) {
                sendJson(exchange, 415, jsonError("نوع رسانه پشتیبانی‌نشده"));
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
                sendJson(exchange, 403, jsonError("فقط فروشندگان مجاز به به‌روزرسانی آیتم منو هستند"));
                return;
            }

            // بررسی نوع کاربر (Seller)
            if (!(user instanceof Seller)) {
                sendJson(exchange, 403, jsonError("کاربر باید از نوع فروشنده باشد"));
                return;
            }

            // استخراج شناسه رستوران و آیتم از مسیر
            String path = exchange.getRequestURI().getPath();
            String[] segments = path.split("/");
            if (segments.length != 5 || !segments[3].equals("item")) {
                sendJson(exchange, 400, jsonError("شناسه رستوران یا آیتم نامعتبر است"));
                return;
            }
            Long restaurantId;
            Long itemId;
            try {
                restaurantId = Long.parseLong(segments[2]);
                itemId = Long.parseLong(segments[4]);
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, jsonError("شناسه رستوران یا آیتم باید عدد باشد"));
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

            // بررسی وجود حداقل یک فیلد برای به‌روزرسانی
            if (body.isEmpty()) {
                sendJson(exchange, 400, jsonError("حداقل یک فیلد برای به‌روزرسانی باید ارائه شود"));
                return;
            }

            // دریافت رستوران و آیتم و بررسی مالکیت
            try (Session session = sessionFactory.openSession()) {
                Restaurant restaurant = session.get(Restaurant.class, restaurantId);
                if (restaurant == null) {
                    sendJson(exchange, 404, jsonError("رستوران یافت نشد"));
                    return;
                }
                if (!restaurant.getSeller().getId().equals(user.getId())) {
                    sendJson(exchange, 403, jsonError("شما مجاز به به‌روزرسانی آیتم این رستوران نیستید"));
                    return;
                }

                MenuItem item = session.get(MenuItem.class, itemId);
                if (item == null || !item.getRestaurant().getId().equals(restaurantId)) {
                    sendJson(exchange, 404, jsonError("آیتم یافت نشد"));
                    return;
                }

                // بررسی وجود MenuCategory (اگر menu_title ارائه شده باشد)
                MenuCategory menuCategory = null;
                if (body.containsKey("menu_title")) {
                    String menuTitle = body.get("menu_title").toString().trim();
                    if (!menuTitle.isEmpty()) {
                        menuCategory = session.createQuery("FROM MenuCategory mc WHERE mc.restaurant.id = :restaurantId AND mc.title = :title", MenuCategory.class)
                                .setParameter("restaurantId", restaurantId)
                                .setParameter("title", menuTitle)
                                .uniqueResult();
                        if (menuCategory == null) {
                            sendJson(exchange, 400, jsonError("دسته‌بندی منو با عنوان " + menuTitle + " یافت نشد"));
                            return;
                        }
                    }
                }

                // به‌روزرسانی فیلدهای ارائه‌شده
                session.beginTransaction();
                if (body.containsKey("name")) {
                    String name = body.get("name").toString().trim();
                    if (name.isEmpty()) {
                        sendJson(exchange, 400, jsonError("نام آیتم نمی‌تواند خالی باشد"));
                        return;
                    }
                    item.setName(name);
                }
                if (body.containsKey("description")) {
                    String description = body.get("description").toString().trim();
                    if (description.isEmpty()) {
                        sendJson(exchange, 400, jsonError("توضیحات آیتم نمی‌تواند خالی باشد"));
                        return;
                    }
                    item.setDescription(description);
                }
                if (body.containsKey("price")) {
                    try {
                        int price = ((Number) body.get("price")).intValue();
                        if (price < 0) {
                            sendJson(exchange, 400, jsonError("قیمت نمی‌تواند منفی باشد"));
                            return;
                        }
                        item.setPrice(price);
                    } catch (ClassCastException | NullPointerException e) {
                        sendJson(exchange, 400, jsonError("مقدار price نامعتبر است"));
                        return;
                    }
                }
                if (body.containsKey("supply")) {
                    try {
                        int supply = ((Number) body.get("supply")).intValue();
                        if (supply < 0) {
                            sendJson(exchange, 400, jsonError("موجودی نمی‌تواند منفی باشد"));
                            return;
                        }
                        item.setSupply(supply);
                    } catch (ClassCastException | NullPointerException e) {
                        sendJson(exchange, 400, jsonError("مقدار supply نامعتبر است"));
                        return;
                    }
                }
                if (body.containsKey("keywords")) {
                    try {
                        List<String> keywords = (List<String>) body.get("keywords");
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
                        item.setKeywords(keywords);
                    } catch (ClassCastException e) {
                        sendJson(exchange, 400, jsonError("فرمت keywords نامعتبر است"));
                        return;
                    }
                }
                if (body.containsKey("imageBase64")) {
                    String imageBase64 = body.get("imageBase64").toString().trim();
                    if (!imageBase64.isEmpty()) {
                        try {
                            item.setImageBase64(imageBase64);
                        } catch (IllegalArgumentException e) {
                            sendJson(exchange, 400, jsonError("فرمت imageBase64 نامعتبر است"));
                            return;
                        }
                    } else {
                        item.setImageBase64(null);
                    }
                }

                // به‌روزرسانی دسته‌بندی منو (اگر menu_title ارائه شده باشد)
                if (menuCategory != null) {
                    // حذف آیتم از دسته‌بندی‌های قبلی
                    session.createQuery("DELETE FROM menu_items_mapping WHERE menu_item_id = :itemId")
                            .setParameter("itemId", itemId)
                            .executeUpdate();
                    // افزودن آیتم به دسته‌بندی جدید
                    menuCategory.addItem(item);
                    session.merge(menuCategory);
                }

                // ذخیره تغییرات در پایگاه داده
                try {
                    session.merge(item);
                    session.getTransaction().commit();
                } catch (ConstraintViolationException e) {
                    session.getTransaction().rollback();
                    sendJson(exchange, 409, jsonError("آیتمی با این نام در منوی رستوران وجود دارد"));
                    return;
                } catch (Exception e) {
                    session.getTransaction().rollback();
                    e.printStackTrace();
                    sendJson(exchange, 500, jsonError("خطای داخلی سرور: " + e.getMessage()));
                    return;
                }

                // به‌روزرسانی منوی رستوران
                restaurant.loadMenu(session);

                // ارسال پاسخ موفقیت‌آمیز
                Map<String, Object> response = Map.of(
                        "id", item.getId(),
                        "name", item.getName(),
                        "description", item.getDescription(),
                        "price", item.getPrice(),
                        "supply", item.getSupply(),
                        "keywords", item.getKeywords(),
                        "imageBase64", item.getImageBase64() != null ? item.getImageBase64() : ""
                );
                sendJson(exchange, 200, gson.toJson(response));
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, jsonError("خطای داخلی سرور: " + e.getMessage()));
        }
    }
}
