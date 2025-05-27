package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.User.*;
import org.example.Security.PasswordUtil;//
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class ProfileApi implements HttpHandler {

    private final SessionFactory sessionFactory;

    public ProfileApi(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")
                    && !exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.contains("application/json")) {
                sendResponse(exchange, 400, "Content-Type must be application/json");
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, "Internal Server Error");
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