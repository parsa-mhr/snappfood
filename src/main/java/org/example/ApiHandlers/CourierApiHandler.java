package org.example.ApiHandlers;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.Services.DeliveryService;
import org.example.Models.OrderResponseDto;
import org.example.Models.UpdateDeliveryStatusRequest;
import com.google.gson.Gson;
import org.example.User.User;
import org.example.Validation.TokenUserValidator;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.example.ApiHandlers.HttpUtils.*;

public class CourierApiHandler {
    private static final Gson gson = new Gson();
    private static final DeliveryService deliveryService = new DeliveryService();

    /**
     * GET /deliveries/available
     */
    public static class AvailableHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendEmpty(exchange, 405);
                return;
            }
            try {
                List<OrderResponseDto> list = deliveryService.getAvailableDeliveries();
                sendJson(exchange, 200, list);
            }catch (Exception e){
                sendError(exchange , 400 , e.getMessage());
            }
        }
    }

    /**
     * GET /deliveries/history/?search= , ,
     */
    public static class HistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendEmpty(exchange, 405);
                return;
            }
            TokenUserValidator validator = new TokenUserValidator();
            User user = validator.validate(exchange.getRequestHeaders().getFirst("Authorization").replace("Bearer " , ""));
            if (user == null)
                sendError(exchange , 401 , "token is not authorized");
            else {
                String query = exchange.getRequestURI().getQuery();
                List<OrderResponseDto> list = deliveryService.getDeliveryHistory(query , user.getId());
                sendJson(exchange, 200, list);
            }
        }
    }

    /**
     * PATCH /deliveries/{order_id}
     * body: { "status": "..." }
     */
    public static class UpdateStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("PATCH")) {
                sendEmpty(exchange, 405);
                return;
            }
            TokenUserValidator validator = new TokenUserValidator();
            User user = validator.validate(exchange.getRequestHeaders().getFirst("Authorization").replace("Bearer " , ""));
            if (user == null)
                sendError(exchange , 401 , "token is not authorized");

            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length < 3) {
                sendError(exchange, 400, "Order ID missing");
                return;
            }
            String orderId = parts[2];

            UpdateDeliveryStatusRequest req;
            try {
                req = gson.fromJson(new InputStreamReader(exchange.getRequestBody()), UpdateDeliveryStatusRequest.class);
            } catch (JsonSyntaxException e) {
                sendError(exchange, 400, "Invalid JSON payload");
                return;
            }

            try {
                OrderResponseDto updated = deliveryService.updateDeliveryStatus(orderId, req.getStatus() , user.getId());
                sendJson(exchange, 200, updated);
            } catch (RuntimeException e) {
                sendError(exchange, 404, e.getMessage());
            }
        }
    }
}
