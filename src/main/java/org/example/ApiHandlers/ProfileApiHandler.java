package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.Security.jwtSecurity;
import org.example.User.Courier;
import org.example.User.Seller;
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

import static org.example.ApiHandlers.SendJson.jsonError;
import static org.example.ApiHandlers.SendJson.sendJson;

public class ProfileApiHandler implements HttpHandler {

    private final SessionFactory sessionFactory;

    public ProfileApiHandler(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization").replace("Bearer ", "");

            // اعتبارسنجی توکن
            User user = null;
            try {
                TokenUserValidator validator = new TokenUserValidator(sessionFactory);
                user = validator.validate(authHeader);

            } catch (Exception e) {
                sendJson(exchange, 401, e.getMessage());
            }
            if (user != null) {
            // هدایت به متد مربوطه
            if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                handleGetProfile(exchange, user);
            } else if (exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
                handleUpdateProfile(exchange, user);
            } else {
                sendJson(exchange, 405, jsonError("Method Not Allowed"));
            }
        }

            } catch(UnauthorizedException e){
                sendJson(exchange, 401, jsonError(e.getMessage()));
            } catch(Exception e){
                e.printStackTrace();
                sendJson(exchange, 500, jsonError("Internal Server Error"));
            }
        }


    // 📥 متد برای به‌روزرسانی پروفایل
    private void handleUpdateProfile(HttpExchange exchange, User user) {
        try (Session session = sessionFactory.openSession()) {
            Map<String, Object> body = new Gson().fromJson(new InputStreamReader(exchange.getRequestBody()), Map.class);

            session.beginTransaction();

            if (body.containsKey("full_name")&& !body.get("full_name").toString().equals("") && body.get("full_name") != null)
                user.setFullName(body.get("full_name").toString());
            if (body.containsKey("phone")&& !body.get("phone").toString().equals("") && body.get("phone") != null)
                user.setPhonenumber(body.get("phone").toString());
            if (body.containsKey("email")&& !(body.get("email").toString().equals("")) && body.get("email") != null)
                user.setEmail(body.get("email").toString());
            if (body.containsKey("address")&& !body.get("address").toString().equals("") && body.get("address") != null)
                user.setadress(body.get("address").toString());
            if (body.containsKey("profileImageBase64") && body.get("profileImageBase64") != null && !body.get("profileImageBase64").toString().equals("") )
                user.setProfileImageBase64(body.get("profileImageBase64").toString());

            // فقط برای Courier
            if (user instanceof Courier courier && body.containsKey("bank_info")) {
                Map<String, Object> bankMap = (Map<String, Object>) body.get("bank_info");
                if (bankMap.containsKey("bank_name") && bankMap.containsKey("account_number") && bankMap.get("account_number") != null  && bankMap.get("bank_name") != null && !bankMap.get("account_number").toString().equals("")  && !bankMap.get("bank_name").toString().equals("")) {
                    courier.setBankInformation(new BankInfo(
                            bankMap.get("bank_name").toString(),
                            bankMap.get("account_number").toString()));
                }
            }else  if (user instanceof Seller seller && body.containsKey("bank_info")) {
                Map<String, Object> bankMap = (Map<String, Object>) body.get("bank_info");
                if (bankMap.containsKey("bank_name") && bankMap.containsKey("account_number") && !body.get("account_number").toString().equals("") && body.get("account_number") != null && !body.get("bank_name").toString().equals("") && body.get("bank_name") != null) {
                    seller.setBankInformation(new BankInfo(
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
}


