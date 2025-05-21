package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.Security.PasswordUtil;
import org.example.User.User;
import org.example.User.UserRole;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LoginApiHandler implements HttpHandler {

    private final SessionFactory sessionFactory;

    public LoginApiHandler(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJson(exchange, 405, "Method Not Allowed");
                return;
            }

            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.contains("application/json")) {
                sendJson(exchange, 400, "Content-Type must be application");
                return;
            }

            Gson gson = new Gson();
            Map<String, String> body = gson.fromJson(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), Map.class);

            String phone = body.get("phone");
            String password = body.get("password");

            if (phone == null || password == null) {
                sendJson(exchange, 400, "Phone and password are required");
                return;
            }

            Session session = sessionFactory.openSession();
            User user = session.createQuery("FROM User WHERE phonenumber = :phone", User.class)
                    .setParameter("phone", phone)
                    .uniqueResult();
            session.close();

            if (user == null || !PasswordUtil.checkPassword(password, user.getPassword())) {
                sendJson(exchange, 401, "Invalid phone or password");
                return;
            }

            String json = gson.toJson(Map.of(
                    "message", "Login successful",
                    "userId", String.valueOf(user.getId()),
                    "role", user.getRole().name(),
                    "token", "fake-jwt-token"
            ));
            sendJson(exchange, 200, json);

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, "Internal Server Error");
        }
    }

    private void sendJson(HttpExchange exchange, int code, String json) {
        try {
            byte[] res = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(code, res.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(res);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
