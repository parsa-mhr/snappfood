package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.User.Courier;
import org.example.User.User;
import org.example.Validation.ExistUser;
import org.example.Security.jwtSecurity;
import org.example.Unauthorized.UnauthorizedException;
import org.hibernate.SessionFactory;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
                sendJson(exchange, 405, jsonError("Method Not Allowed"));
                return;
            }

            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.contains("application/json")) {
                sendJson(exchange, 400, jsonError("Content-Type must be application/json"));
                return;
            }

            Gson gson = new Gson();
            Map<String, String> body = gson.fromJson(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), Map.class);

            String phone = body.get("phone");
            String password = body.get("password");

            // استفاده از کلاس ExistUser برای بررسی کاربر
            ExistUser checker = new ExistUser(sessionFactory);
            User user = checker.validate(phone, password);

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", String.valueOf(user.getId()));
            userMap.put("full_name", user.getFullName());
            userMap.put("phone", user.getPhonenumber());
            userMap.put("email", user.getEmail());
            userMap.put("role", user.getRole().name());
            userMap.put("address", user.getadress());
            userMap.put("profileImageBase64", user.getProfileImageBase64());

            if (user instanceof Courier courier && courier.getBankInformation() != null) {
                Map<String, String> bankMap = new HashMap<>();
                bankMap.put("bank_name", courier.getBankInformation().getBankName());
                bankMap.put("account_number", courier.getBankInformation().getAccountNumber());
                userMap.put("bank_info", bankMap);
            }

            String token = jwtSecurity.generateToken(user.getId(), user.getRole().name());
            Long userId = user.getId(); // ⬅️ فقط بعد از save استفاده کن

            Map<String, Object> responseMap = new LinkedHashMap<>();
            responseMap.put("message", "Login successful");
            responseMap.put("token", token);
            responseMap.put("user", userMap);
            String json = gson.toJson(responseMap);
            sendJson(exchange, 200, json);

        } catch (UnauthorizedException e) {
            sendJson(exchange, 401, jsonError(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, jsonError("Internal Server Error"));
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

    private String jsonError(String msg) {
        return new Gson().toJson(Map.of("error", msg));
    }
}
