package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.Security.PasswordUtil;
import org.example.User.Courier;
import org.example.User.User;
import org.example.User.UserRole;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
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
                sendJson(exchange, 405, jsonMessage("Method Not Allowed"));
                return;
            }

            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.contains("application/json")) {
                sendJson(exchange, 400, jsonMessage("Content-Type must be application/json"));
                return;
            }

            Gson gson = new Gson();
            Map<String, String> body = gson.fromJson(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), Map.class);

            String phone = body.get("phone");
            String password = body.get("password");

            if (phone == null || password == null) {
                sendJson(exchange, 400, jsonMessage("Phone and password are required"));
                return;
            }

            Session session = sessionFactory.openSession();
            User user = session.createQuery("FROM User WHERE phonenumber = :phone", User.class)
                    .setParameter("phone", phone)
                    .uniqueResult();
            session.close();

            if (user == null || !PasswordUtil.checkPassword(password, user.getPassword())) {
                sendJson(exchange, 401, jsonMessage("Invalid phone or password"));
                return;
            }

            Map<String, Object> userMap = new LinkedHashMap<>();
            userMap.put("id", user.getId());
            userMap.put("full_name", user.getFullName());
            userMap.put("phone", user.getPhonenumber());
            userMap.put("email", user.getEmail());
            userMap.put("role", user.getRole().name());
            userMap.put("address", user.getadress());
            userMap.put("profileImageBase64", user.getProfileImageBase64());

            // اگر اطلاعات بانکی داشته باشه (مثلاً courier)
            if (user instanceof Courier courier && courier.getBankInformation() != null) {
                Map<String, String> bankMap = new LinkedHashMap<>();
                bankMap.put("bank_name", courier.getBankInformation().getBankName());
                bankMap.put("account_number", courier.getBankInformation().getAccountNumber());
                userMap.put("bank_info", bankMap);
            }

            Map<String, Object> responseMap = new LinkedHashMap<>();
            responseMap.put("message", "Login successful");
            responseMap.put("token", "fake-jwt-token"); // توکن واقعی در نسخه نهایی
            responseMap.put("user", userMap);

            String json = gson.toJson(responseMap);
            sendJson(exchange, 200, json);

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, jsonMessage("Internal Server Error"));
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

    private String jsonMessage(String msg) {
        return new Gson().toJson(Map.of("error", msg));
    }
}
