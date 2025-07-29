package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.DAO.OrderDAO;
import org.example.DAO.WalletDAO;
import org.example.Models.MenuItemDto;
import org.example.Models.UserStatusRequest;
import org.example.Restaurant.Menu;
import org.example.Restaurant.MenuCategory;
import org.example.Restaurant.MenuCategoryDTO;
import org.example.Restaurant.Restaurant;
import org.example.Services.*;
import org.example.User.User;
import org.example.User.UserResponseDto;
import org.example.Validation.TokenUserValidator;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static org.example.ApiHandlers.SendJson.sendJson;

public class AdminApiHandlers {

    private static final SessionFactory sessionFactory;
    private static final UserService userService;
    private static final OrderService orderService;
    private static final OrderDAO orderDAO;
    private static final WalletDAO walletDAO;
    private static final Gson gson;


    static {
        sessionFactory = new org.hibernate.cfg.Configuration().configure().buildSessionFactory();
        userService = new UserService(sessionFactory);
        orderDAO = new OrderDAO(sessionFactory);
        orderService = new OrderService();
        walletDAO = new WalletDAO(sessionFactory);
        gson = new Gson();
    }

    //admin/menus GET
    public static class AdminMenusListHandler implements HttpHandler {
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
//                System.out.println("Fetching all users..."); // Debug log
               try (Session session =sessionFactory.openSession()){
                   List<MenuCategory> menuCategory = session.createQuery(
                                   "FROM MenuCategory ",
                                   MenuCategory.class)
                           .list();

                   List<MenuAdminResponse> categoryDTOs = menuCategory.stream()
                           .map(MenuAdminResponse::new)
                           .collect(Collectors.toList());


                   // ارسال پاسخ موفقیت‌آمیز
                   sendJson(exchange, 200, gson.toJson(categoryDTOs));

               }
            } catch (Exception e) {
                System.err.println("Error in AdminUsersListHandler: " + e.getMessage());
                e.printStackTrace();
                HttpUtils.sendError(exchange, 500, e.getMessage());
            }
        }
    }
    //admin/restaurants GET
    public static class AdminRestaurantsListHandler implements HttpHandler {
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
//                System.out.println("Fetching all users..."); // Debug log
                try (Session session =sessionFactory.openSession()){
                    List<Restaurant> restaurants = session.createQuery(
                                    "FROM Restaurant ",
                                    Restaurant.class)
                            .list();

                    List<RestaurantAdminResponse> categoryDTOs = restaurants.stream()
                            .map(RestaurantAdminResponse::new)
                            .collect(Collectors.toList());


                    // ارسال پاسخ موفقیت‌آمیز
                    sendJson(exchange, 200, gson.toJson(categoryDTOs));

                }
            } catch (Exception e) {
                System.err.println("Error in AdminUsersListHandler: " + e.getMessage());
                e.printStackTrace();
                HttpUtils.sendError(exchange, 500, e.getMessage());
            }
        }
    }
    // PATCH /admin/users/{id}/status
    public static class AdminRestaurantStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("PATCH")) {
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
                String[] pathParts = exchange.getRequestURI().getPath().split("/");
                if (pathParts.length < 4) {
                    HttpUtils.sendError(exchange, 400, "Invalid user ID");
                    return;
                }
                long RestaurantId = Long.parseLong(pathParts[pathParts.length - 2]);
                UserStatusRequest request = gson.fromJson(new InputStreamReader(exchange.getRequestBody()), UserStatusRequest.class);
                if (request == null || request.getStatus() == null) {
                    HttpUtils.sendError(exchange, 400, "Invalid payload: status is required");
                    return;
                }
                try(Session session = sessionFactory.openSession()) {
                  Restaurant r =  session.find(Restaurant.class , RestaurantId) ;
                  if (r ==  null)
                      throw new NumberFormatException();
                  if (!("approved".equals(request.getStatus()) || "rejected".equals(request.getStatus())))
                      throw new JsonSyntaxException("wrong status");
                  r.setStatus(request.getStatus());
                    session.beginTransaction();
                    session.merge(r);
                    session.getTransaction().commit();
                    HttpUtils.sendJson(exchange, 200, new TransactionsApiHandlers.SuccessResponse("Status updated"));
                }
                HttpUtils.sendJson(exchange, 200, new TransactionsApiHandlers.SuccessResponse("Status updated"));
            } catch (JsonSyntaxException e) {
                HttpUtils.sendError(exchange, 400, "Invalid JSON payload");
            } catch (NumberFormatException e) {
                HttpUtils.sendError(exchange, 400, "Invalid Restaurant ID");
            } catch (Exception e) {
                System.err.println("Error in AdminUserStatusHandler: " + e.getMessage());
                e.printStackTrace();
                HttpUtils.sendError(exchange, 400, e.getMessage());
            }
        }
    }
    public static class AdminMenuStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("PATCH")) {
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
                String[] pathParts = exchange.getRequestURI().getPath().split("/");
                if (pathParts.length < 4) {
                    HttpUtils.sendError(exchange, 400, "Invalid user ID");
                    return;
                }
                long MenuId = Long.parseLong(pathParts[pathParts.length - 2]);
                UserStatusRequest request = gson.fromJson(new InputStreamReader(exchange.getRequestBody()), UserStatusRequest.class);
                if (request == null || request.getStatus() == null) {
                    HttpUtils.sendError(exchange, 400, "Invalid payload: status is required");
                    return;
                }
                try(Session session = sessionFactory.openSession()) {
                    MenuCategory r =  session.find(MenuCategory.class , MenuId) ;
                    if (r ==  null)
                        throw new NumberFormatException();
                    if (!("approved".equals(request.getStatus()) || "rejected".equals(request.getStatus())))
                        throw new JsonSyntaxException("wrong status");
                    r.setStatus(request.getStatus());
                    session.beginTransaction();
                    session.merge(r);
                    session.getTransaction().commit();
                    HttpUtils.sendJson(exchange, 200, new TransactionsApiHandlers.SuccessResponse("Status updated"));
                }
                HttpUtils.sendJson(exchange, 200, new TransactionsApiHandlers.SuccessResponse("Status updated"));
            } catch (JsonSyntaxException e) {
                HttpUtils.sendError(exchange, 400, "Invalid JSON payload");
            } catch (NumberFormatException e) {
                HttpUtils.sendError(exchange, 400, "Invalid Restaurant ID");
            } catch (Exception e) {
                System.err.println("Error in AdminUserStatusHandler: " + e.getMessage());
                e.printStackTrace();
                HttpUtils.sendError(exchange, 400, e.getMessage());
            }
        }
    }

    public static class MenuAdminResponse {
        private Long id;
        private String title;
        private List<MenuItemDto> items;
        private String status ;
        private String restaurantName ;

        public MenuAdminResponse(MenuCategory category) {
            this.id = category.getId();
            this.title = category.getTitle();
            this.items = category.getItems()
                    .stream()
                    .map(MenuItemDto::new)
                    .collect(Collectors.toList());
            this.status = category.getStatus() != null ? category.getStatus() : "null";
            this.restaurantName = category.getRestaurant().getName();
        }
    }
    public static class RestaurantAdminResponse{
        private long id;
        private String name;
        private String address;
        private String phone;
        private String logoBase64;
        private double tax_fee;
        private double additional_fee;
        private String status ;

        public RestaurantAdminResponse(Restaurant res){
            this.id = res.getId();
            this.name = res.getName();
            this.status = res.getStatus() == null ? "null" : res.getStatus() ;
            this.phone = res.getPhone();
            this.logoBase64 = res.getLogoBase64();
            this.tax_fee = res.getTaxFee();
            this.additional_fee = res.getAdditional_fee();
            this.address = res.getAddress();
        }

    }

}
