package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.Security.jwtSecurity;
import org.example.User.Courier;
import org.example.Validation.TokenUserValidator;
import org.example.User.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.example.Unauthorized.UnauthorizedException;
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
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendJson(exchange, 405, jsonError("Method Not Allowed"));
                return;
            }

            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

            // 🎯 انتقال تمام اعتبارسنجی‌ها به این کلاس
            TokenUserValidator validator = new TokenUserValidator(sessionFactory);
            User user = validator.validate(authHeader);

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

            String json = new Gson().toJson(userMap);
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
