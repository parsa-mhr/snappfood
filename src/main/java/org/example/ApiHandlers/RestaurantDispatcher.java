package org.example.ApiHandlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.hibernate.SessionFactory;

import static org.example.ApiHandlers.SendJson.jsonError;
import static org.example.ApiHandlers.SendJson.sendJson;

/**
 * کلاس RestaurantDispatcher برای هدایت درخواست‌های مرتبط با رستوران‌ها
 */
public class RestaurantDispatcher implements HttpHandler {
    private final SessionFactory sessionFactory;

    public RestaurantDispatcher(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            String path = exchange.getRequestURI().getPath();
            String[] segments = path.split("/");

            // بررسی مسیرهای معتبر
            if (segments.length < 2 || !segments[1].equals("restaurants")) {
                sendJson(exchange, 400, jsonError("مسیر نامعتبر است"));
                return;
            }

            // هدایت درخواست‌ها
            if (segments.length == 4 && segments[3].equals("orders")) {
                // برای GET /restaurants/{id}/orders
                if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                    new RestaurantOrdersApiHandler(sessionFactory).handle(exchange);
                } else {
                    sendJson(exchange, 405, jsonError("متد مجاز نیست"));
                }
            } else if (segments.length == 4 && segments[2].equals("orders")) {
                // برای PATCH /restaurants/orders/{order_id}
                if (exchange.getRequestMethod().equalsIgnoreCase("PATCH")) {
                    new RestaurantOrderStatusUpdateApiHandler(sessionFactory).handle(exchange);
                } else {
                    sendJson(exchange, 405, jsonError("متد مجاز نیست"));
                }
            } else {
                sendJson(exchange, 400, jsonError("مسیر نامعتبر است"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, jsonError("خطای داخلی سرور: " + e.getMessage()));
        }
    }
}
