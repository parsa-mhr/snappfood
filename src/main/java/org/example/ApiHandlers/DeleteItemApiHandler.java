package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.Restaurant.MenuItem;
import org.example.Restaurant.Restaurant;
import org.example.User.User;
import org.example.Validation.TokenUserValidator;
import org.example.Unauthorized.UnauthorizedException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import java.util.Map;


import static org.example.ApiHandlers.SendJson.jsonError;
import static org.example.ApiHandlers.SendJson.sendJson;

/**
 * کلاس DeleteItemApiHandler برای مدیریت درخواست‌های DELETE به endpoint /restaurants/{id}/item/{item_id}
 * این endpoint برای حذف یک آیتم از منوی رستوران استفاده می‌شود
 */
public class DeleteItemApiHandler implements HttpHandler {
    private final SessionFactory sessionFactory; // فکتوری برای مدیریت سشن‌های Hibernate
    private final Gson gson; // شیء Gson برای تبدیل اشیا به JSON

    /**
     * سازنده کلاس که SessionFactory را دریافت می‌کند
     * @param sessionFactory فکتوری برای ارتباط با پایگاه داده
     */
    public DeleteItemApiHandler(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.gson = new Gson();
    }

    /**
     * متد اصلی برای مدیریت درخواست‌های HTTP
     * @param exchange شیء HttpExchange برای مدیریت درخواست و پاسخ
     */
    @Override
    public void handle(HttpExchange exchange) {
        try {
            // بررسی اینکه متد درخواست DELETE باشد
            if (!exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
                sendJson(exchange, 405, jsonError("متد مجاز نیست"));
                return;
            }

            // استخراج هدر Authorization
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null) {
                sendJson(exchange, 401, jsonError("هدر Authorization وجود ندارد"));
                return;
            }

            // اعتبارسنجی توکن و دریافت کاربر
            TokenUserValidator validator = new TokenUserValidator(sessionFactory);
            User user;
            try {
                user = validator.validate(authHeader);
            } catch (UnauthorizedException e) {
                sendJson(exchange, 401, jsonError(e.getMessage()));
                return;
            }

            // بررسی نقش کاربر (فقط فروشنده مجاز است)
            if (!user.getRole().equals("seller")) {
                sendJson(exchange, 403, jsonError("فقط فروشندگان مجاز به حذف آیتم منو هستند"));
                return;
            }

            // استخراج شناسه رستوران و آیتم از مسیر
            String path = exchange.getRequestURI().getPath();
            String[] segments = path.split("/");
            if (segments.length < 5 || !segments[3].equals("item")) {
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

            // دریافت رستوران و آیتم و بررسی مالکیت
            try (Session session = sessionFactory.openSession()) {
                Restaurant restaurant = session.get(Restaurant.class, restaurantId);
                if (restaurant == null) {
                    sendJson(exchange, 404, jsonError("رستوران یافت نشد"));
                    return;
                }
                if (!restaurant.getSeller().getId().equals(user.getId())) {
                    sendJson(exchange, 403, jsonError("شما مجاز به حذف آیتم این رستوران نیستید"));
                    return;
                }

                MenuItem item = session.get(MenuItem.class, itemId);
                if (item == null || !item.getRestaurant().getId().equals(restaurantId)) {
                    sendJson(exchange, 404, jsonError("آیتم یافت نشد"));
                    return;
                }

                // حذف آیتم از پایگاه داده
                try {
                    session.beginTransaction();
                    session.delete(item);
                    session.getTransaction().commit();
                } catch (Exception e) {
                    session.getTransaction().rollback();
                    e.printStackTrace();
                    sendJson(exchange, 500, jsonError("خطای داخلی سرور"));
                    return;
                }

                // به‌روزرسانی منوی رستوران
                restaurant.loadMenu(session);

                // ارسال پاسخ موفقیت‌آمیز
                sendJson(exchange, 200, gson.toJson(Map.of(
                        "message", "آیتم با موفقیت از منو حذف شد")));
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, jsonError("خطای داخلی سرور"));
        }
    }
}
