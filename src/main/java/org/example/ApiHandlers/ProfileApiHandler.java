package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.Security.jwtSecurity;
import org.example.User.Courier;
import org.example.User.User;
import org.example.User.BankInfo;
import org.example.Validation.TokenUserValidator;
import org.example.Unauthorized.UnauthorizedException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProfileApiHandler implements HttpHandler {

    private final SessionFactory sessionFactory;

    public ProfileApiHandler(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

            // اعتبارسنجی توکن
            TokenUserValidator validator = new TokenUserValidator(sessionFactory);
            User user = validator.validate(authHeader);

            // هدایت به متد مربوطه
            if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                handleGetProfile(exchange, user);
            } else if (exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
                handleUpdateProfile(exchange, user);
            } else {
                sendJson(exchange, 405, jsonError("Method Not Allowed"));
            }

        } catch (UnauthorizedException e) {
            sendJson(exchange, 401, jsonError(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, jsonError("Internal Server Error"));
        }
    }

    // 📥 متد برای به‌روزرسانی پروفایل
    private void handleUpdateProfile(HttpExchange exchange, User user) {
        try (Session session = sessionFactory.openSession()) {
            Map<String, Object> body = new Gson().fromJson(new InputStreamReader(exchange.getRequestBody()), Map.class);

            session.beginTransaction();

            if (body.containsKey("full_name"))
                user.setFullName(body.get("full_name").toString());
            if (body.containsKey("phone"))
                user.setPhonenumber(body.get("phone").toString());
            if (body.containsKey("email"))
                user.setEmail(body.get("email").toString());
            if (body.containsKey("address"))
                user.setadress(body.get("address").toString());
            if (body.containsKey("profileImageBase64"))
                user.setProfileImageBase64(body.get("profileImageBase64").toString());

            // فقط برای Courier
            if (user instanceof Courier courier && body.containsKey("bank_info")) {
                Map<String, Object> bankMap = (Map<String, Object>) body.get("bank_info");
                if (bankMap.containsKey("bank_name") && bankMap.containsKey("account_number")) {
                    courier.setBankInformation(new BankInfo(
                            bankMap.get("bank_name").toString(),
                            bankMap.get("account_number").toString()));
                }
            }

            session.update(user);
            session.getTransaction().commit();

            sendJson(exchange, 200, new Gson().toJson(Map.of("message", "Profile updated successfully")));

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 400, jsonError("Invalid input"));
        }
    }

    // 📤 متد برای دریافت اطلاعات پروفایل
    private void handleGetProfile(HttpExchange exchange, User user) {
        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("id", String.valueOf(user.getId()));
        userMap.put("full_name", user.getFullName());
        userMap.put("phone", user.getPhonenumber());
        userMap.put("email", user.getEmail());
        userMap.put("role", user.getRole().name());
        userMap.put("address", user.getadress());
        userMap.put("profileImageBase64", user.getProfileImageBase64());

        if (user instanceof Courier courier && courier.getBankInformation() != null) {
            Map<String, String> bankMap = new LinkedHashMap<>();
            bankMap.put("bank_name", courier.getBankInformation().getBankName());
            bankMap.put("account_number", courier.getBankInformation().getAccountNumber());
            userMap.put("bank_info", bankMap);
        }

        sendJson(exchange, 200, new Gson().toJson(userMap));
    }

    // 📦 ارسال پاسخ JSON
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

    // 📦 ساخت ارور JSON
    private String jsonError(String msg) {
        return new Gson().toJson(Map.of("error", msg));
    }
}
