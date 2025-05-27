package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.User.*;
import org.example.Security.PasswordUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class RegisterApiHandler implements HttpHandler {

    private final SessionFactory sessionFactory;

    public RegisterApiHandler(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.contains("application/json")) {
                sendResponse(exchange, 400, "Content-Type must be application/json");
                return;
            }

            Gson gson = new Gson();
            Map<String, Object> body = gson.fromJson(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), Map.class);

            // فیلدهای اجباری
            String fullName = (String) body.get("fullName");
            String email = (String) body.get("email");
            String password = (String) body.get("password");
            String phone = (String) body.get("phone");
            String address = (String) body.get("address");
            String role = (String) body.get("role");
            String profileImageBase64 = (String) body.get("profileImageBase64");

            if (fullName == null || password == null ||
                    phone == null || address == null || role == null) {
                sendResponse(exchange, 400, "Missing required fields");
                return;
            }

            byte[] imageBytes = null;
            if (profileImageBase64 != null) {
                try {
                    imageBytes = Base64.getDecoder().decode(profileImageBase64);
                } catch (Exception e) {
                    sendResponse(exchange, 400, "Invalid base64 for image");
                    return;
                }
            }

            // ساخت یوزر با توجه به نقش
            User user;
            switch (role) {

                case "seller" -> {
                    String hashedPassword = PasswordUtil.hashPassword(password);
                    user = new Seller(fullName, email, hashedPassword, phone, address);
                    user.setRole(UserRole.valueOf(role));

                }
                case "courier" -> {
                    String hashedPassword = PasswordUtil.hashPassword(password);
                    Map<String, Object> bankInfoMap = (Map<String, Object>) body.get("bankInfo");

                    if (bankInfoMap == null || bankInfoMap.get("accountNumber") == null
                            || bankInfoMap.get("bankName") == null) {
                        sendResponse(exchange, 400, "Missing bankInformation for courier");
                        return;
                    }

                    BankInfo bankInfo = new BankInfo(
                            (String) bankInfoMap.get("bankName"),
                            (String) bankInfoMap.get("accountNumber"));

                    user = new Courier(fullName, email, hashedPassword, phone, address, bankInfo);
                    user.setRole(UserRole.valueOf(role));

                }

                case "buyer" -> {
                    String hashedPassword = PasswordUtil.hashPassword(password);
                    user = new Buyer(fullName, email, hashedPassword, phone, address);
                    user.setRole(UserRole.valueOf(role));

                }

                default -> {
                    sendResponse(exchange, 400, "Invalid role");
                    return;
                }
            }

            user.setImage(imageBytes);
            user.setProfileImageBase64(profileImageBase64);

            Session session = sessionFactory.openSession();
            Transaction tx = session.beginTransaction();
            User existingUser = session.createQuery("FROM User WHERE phonenumber = :phone", User.class)
                    .setParameter("phone", phone)
                    .uniqueResult();

            if (existingUser != null) {
                session.close();
                sendJson(exchange, 409, "Phone number already exist");
                return;
            }
            session.save(user);
            tx.commit();
            session.close();

            String json = gson.toJson(Map.of(
                    "message", "User registered successfully",
                    "userId", user.getId(),
                    "token", "fake-jwt-token"));
            sendJson(exchange, 200, json);

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 400, "Invalid input");
        }
    }

    private void sendResponse(HttpExchange exchange, int code, String msg) {
        try {
            byte[] res = msg.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
            exchange.sendResponseHeaders(code, res.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(res);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendJson(HttpExchange exchange, int code, String json) {
        try {
            byte[] res = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(code, res.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(res);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
