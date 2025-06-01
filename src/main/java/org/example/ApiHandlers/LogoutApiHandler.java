package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.Security.TokenBlacklist;
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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.example.ApiHandlers.SendJson.jsonError;
import static org.example.ApiHandlers.SendJson.sendJson;


public class LogoutApiHandler implements HttpHandler {

    public LogoutApiHandler() {
    }

    @Override
    public void handle(HttpExchange exchange) {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendJson(exchange, 405, jsonError("Method Not Allowed"));
            return;
        }
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            SendJson.sendJson(exchange, 401, jsonError("Unauthorized"));
            return;
        }
        String token = authHeader.substring(7);

        try {
            String jti = jwtSecurity.getJti(token);
            Date expiry = jwtSecurity.getExpiration(token);

            TokenBlacklist.add(jti, expiry);

            sendJson(exchange, 200, "User logged out successfully");
        } catch (Exception e) {
            sendJson(exchange, 401, jsonError("Unauthorized"));
        }
    }
}