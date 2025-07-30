package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.Models.RatingResponseDto;
import org.example.Models.UserStatusRequest;
import org.example.Restaurant.MenuCategory;
import org.example.Services.RatingService;
import org.example.User.User;
import org.example.Validation.TokenUserValidator;
import org.hibernate.Session;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import static org.example.ApiHandlers.AdminApiHandlers.gson;
import static org.example.Main.sessionFactory;
//ratings/mine get
public class MyReviewsAPiHandler  implements HttpHandler {
    RatingService ratingService = new RatingService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            HttpUtils.sendMethodNotAllowed(exchange);
            return;
        }
        try {
            String token = exchange.getRequestHeaders().getFirst("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                HttpUtils.sendError(exchange, 401, "Missing or invalid Authorization header");
                return;
            }
            token = token.replace("Bearer ", "");
            System.out.println("Validating token: " + token); // Debug log
            User user = new TokenUserValidator(sessionFactory).validate(token);
            if (user == null /*|| user.getRole() != UserRole.ADMIN*/) {
                HttpUtils.sendError(exchange, 403, "Access denied: Admin role required");
                return;
            }
            try {
                List<RatingResponseDto> ratings = ratingService.listByUser(Math.toIntExact(user.getId()));

                HttpUtils.sendJson(exchange, 200, ratings);
            } catch (Exception e) {
                HttpUtils.sendError(exchange, 400, e.getMessage());
            }
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        }
    }
}
