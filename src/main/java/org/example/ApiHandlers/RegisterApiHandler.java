package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.DAO.UserDAO;
import org.example.Security.PasswordUtil;
import org.example.User.*;
import org.example.Validation.CheckUser;
import org.example.invalidFieldName.InvalidFieldException;
import org.example.AlredyExist.AlredyExistException;
import org.hibernate.SessionFactory;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.example.Security.jwtSecurity;

public class RegisterApiHandler implements HttpHandler {

    private final SessionFactory sessionFactory;

    public RegisterApiHandler(SessionFactory sessionFactory) {
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
            Map<String, Object> body = gson.fromJson(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), Map.class);

            // استخراج داده‌ها
            String fullName = (String) body.get("fullName");
            String email = (String) body.get("email");
            String password = (String) body.get("password");
            String phone = (String) body.get("phone");
            String address = (String) body.get("address");
            String role = (String) body.get("role");
            String profileImageBase64 = (String) body.get("profileImageBase64");
            String hashedPassword = PasswordUtil.hashPassword(password);

            byte[] imageBytes = null;
            if (profileImageBase64 != null) {
                imageBytes = Base64.getDecoder().decode(profileImageBase64);
            }
            if (password == null) {
                throw new InvalidFieldException("Password");
            }

            User user;

            switch (role) {
                case "seller" -> {
                    user = new Seller(fullName, email, hashedPassword, phone, address);
                    user.setRole(UserRole.seller);
                }
                case "courier" -> {
                    BankInfo bankInfo = null;
                    if (body.get("bankInfo") instanceof Map<?, ?> bankInfoMap) {
                        bankInfo = new BankInfo(
                                (String) bankInfoMap.get("bankName"),
                                (String) bankInfoMap.get("accountNumber"));
                    }
                    user = new Courier(fullName, email, hashedPassword, phone, address, bankInfo);
                    user.setRole(UserRole.courier);
                }
                case "buyer" -> {
                    user = new Buyer(fullName, email, hashedPassword, phone, address);
                    user.setRole(UserRole.buyer);
                }
                default -> {
                    sendJson(exchange, 400, jsonError("Invalid role"));
                    return;
                }
            }

            user.setImage(imageBytes);
            user.setProfileImageBase64(profileImageBase64);

            // اعتبارسنجی متمرکز
            CheckUser validator = new CheckUser(sessionFactory);
            validator.validate(user, profileImageBase64);

            // ذخیره در دیتابیس
            UserDAO dao = new UserDAO(sessionFactory);
            dao.save(user);
            Long userId = user.getId(); // ⬅️ فقط بعد از save استفاده کن
            String token = jwtSecurity.generateToken(user.getId(), user.getRole().name());

            Map<String, Object> responseMap = new LinkedHashMap<>();
            responseMap.put("message", "Login successful");
            responseMap.put("user_id", user.getId());
            responseMap.put("token", token);
            String json = gson.toJson(responseMap);

            // پاسخ موفق
            sendJson(exchange, 200, json);

        } catch (InvalidFieldException | AlredyExistException e) {
            sendJson(exchange, 400, jsonError(e.getMessage()));
        } catch (RuntimeException e) {
            e.printStackTrace();
            sendJson(exchange, 500, jsonError("Database error: " + e.getMessage()));
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
