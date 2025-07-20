package org.example.ApiHandlers;


import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;

public class HttpUtils {

    public static void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        String msg = "Method Not Allowed";
        exchange.sendResponseHeaders(405, msg.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(msg.getBytes());
        }
    }

    public static void sendBadRequest(HttpExchange exchange, String message) throws IOException {
        exchange.sendResponseHeaders(400, message.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(message.getBytes());
        }
    }

    public static void sendNotFound(HttpExchange exchange, String message) throws IOException {
        exchange.sendResponseHeaders(404, message.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(message.getBytes());
        }
    }

    public static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, json.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json.getBytes());
        }
    }
}

