package org.example.ApiHandlers;


import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import org.example.Models.ErrorResponse;
import org.example.Restaurant.Menu;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;


public class HttpUtils {
    private static final Gson gson = new Gson();

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
    public static void sendJson(HttpExchange ex, int code, Object obj) throws IOException {
        String j = gson.toJson(obj);
        byte[] b = j.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(b);
        }
    }

    public static void sendError (HttpExchange ex,int code, String msg) throws IOException {
        sendJson(ex, code, new ErrorResponse(code, msg));
    }
    public static void sendEmpty (HttpExchange ex,int code) throws IOException {
        ex.sendResponseHeaders(code, -1);
    }
    public static int parseId (HttpExchange ex){
        String[] p = ex.getRequestURI().getPath().split("/");
        return Integer.parseInt(p[p.length - 1]);
    }
}

