package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.DAO.OrderDAO;
import org.example.DAO.TransactionDAO;
import org.example.DAO.UserDAO;
import org.example.Details.Cart;
import org.example.Models.*;
import org.example.Services.*;
import org.example.User.*;
import org.example.Validation.TokenUserValidator;
import org.hibernate.SessionFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionsApiHandlers {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    private static final SessionFactory sessionFactory;
    private static final TransactionService transactionService;
    private static final WalletService walletService;
    private static final PaymentService paymentService;
    private static final UserService userService;
    private static final OrderService orderService;
    private static final OrderDAO orderDAO;

    static {
        sessionFactory = new org.hibernate.cfg.Configuration().configure().buildSessionFactory();
        transactionService = new TransactionService(sessionFactory);
        walletService = new WalletService(sessionFactory);
        paymentService = new PaymentService(sessionFactory);
        userService = new UserService(sessionFactory);
        orderDAO = new OrderDAO(sessionFactory);
        orderService = new OrderService();
    }

    // GET /transactions
    public static class TransactionsListHandler implements HttpHandler {
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
                if (user == null || !(user instanceof Buyer || user instanceof Seller || user instanceof Courier)) {
                    HttpUtils.sendError(exchange, 403, "Access denied");
                    return;
                }
                System.out.println("Fetching transactions for user: " + user.getId()); // Debug log
                List<TransactionResponseDto> transactions = transactionService.getUserTransactions(user.getId())
                        .stream()
                        .map(TransactionResponseDto::new)
                        .collect(Collectors.toList());
                HttpUtils.sendJson(exchange, 200, transactions);
            } catch (Exception e) {
                System.err.println("Error in TransactionsListHandler: " + e.getMessage());
                e.printStackTrace();
                HttpUtils.sendError(exchange, 400, e.getMessage());
            }
        }
    }

    // POST /wallet/top-up
    public static class WalletTopUpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
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
                if ( user == null || !(user instanceof Buyer || user instanceof Seller || user instanceof Courier)) {
                    HttpUtils.sendError(exchange, 403, "Access denied");
                    return;
                }
                TopUpRequest request = gson.fromJson(new InputStreamReader(exchange.getRequestBody()), TopUpRequest.class);
                if (request == null || request.getAmount() == null) {
                    HttpUtils.sendError(exchange, 400, "Invalid payload: amount is required");
                    return;
                }
                walletService.topUp(user.getId(), request);
                HttpUtils.sendJson(exchange, 200, new SuccessResponse("Wallet topped up successfully"));
            } catch (JsonSyntaxException e) {
                HttpUtils.sendError(exchange, 400, "Invalid JSON payload");
            } catch (Exception e) {
                System.err.println("Error in WalletTopUpHandler: " + e.getMessage());
                e.printStackTrace();
                HttpUtils.sendError(exchange, 400, e.getMessage());
            }
        }
    }

    // POST /payment/online
    public static class PaymentOnlineHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
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
                if (user == null || !(user instanceof Buyer)) {
                    HttpUtils.sendError(exchange, 403, "Access denied");
                    return;
                }
                PaymentRequest request = gson.fromJson(new InputStreamReader(exchange.getRequestBody()), PaymentRequest.class);
                if (request == null || request.getOrderId() == null || request.getMethod() == null) {
                    HttpUtils.sendError(exchange, 400, "Invalid payload: orderId and method are required");
                    return;
                }
                Transaction transaction = paymentService.processOnlinePayment(user.getId(), request);
                HttpUtils.sendJson(exchange, 200, new TransactionResponseDto(transaction));
            } catch (JsonSyntaxException e) {
                HttpUtils.sendError(exchange, 400, "Invalid JSON payload");
            } catch (Exception e) {
                System.err.println("Error in PaymentOnlineHandler: " + e.getMessage());
                e.printStackTrace();
                HttpUtils.sendError(exchange, 400, e.getMessage());
            }
        }
    }

    // GET /admin/users
    public static class AdminUsersListHandler implements HttpHandler {
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
                List<User> users = userService.getAllUsers();
                List<UserResponseDto> responseDtos = users.stream()
                        .map(UserResponseDto::new)
                        .collect(Collectors.toList());
                HttpUtils.sendJson(exchange, 200, responseDtos);
            } catch (Exception e) {
                System.err.println("Error in AdminUsersListHandler: " + e.getMessage());
                e.printStackTrace();
                HttpUtils.sendError(exchange, 500, e.getMessage());
            }
        }
    }

    // PATCH /admin/users/{id}/status
    public static class AdminUserStatusHandler implements HttpHandler {
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
                long userId = Long.parseLong(pathParts[pathParts.length - 2]);
                UserStatusRequest request = gson.fromJson(new InputStreamReader(exchange.getRequestBody()), UserStatusRequest.class);
                if (request == null || request.getStatus() == null) {
                    HttpUtils.sendError(exchange, 400, "Invalid payload: status is required");
                    return;
                }
                userService.updateUserStatus(userId, request);
                HttpUtils.sendJson(exchange, 200, new SuccessResponse("Status updated"));
            } catch (JsonSyntaxException e) {
                HttpUtils.sendError(exchange, 400, "Invalid JSON payload");
            } catch (NumberFormatException e) {
                HttpUtils.sendError(exchange, 400, "Invalid user ID");
            } catch (Exception e) {
                System.err.println("Error in AdminUserStatusHandler: " + e.getMessage());
                e.printStackTrace();
                HttpUtils.sendError(exchange, 400, e.getMessage());
            }
        }
    }

    // GET /admin/orders
    public static class AdminOrdersListHandler implements HttpHandler {
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
                System.out.println("Fetching all orders..."); // Debug log
                List<OrderResponseDto> orders = orderService.findAll();
                System.out.println("Orders fetched: " + orders.size()); // Debug log
                HttpUtils.sendJson(exchange, 200, orders);
            } catch (Exception e) {
                System.err.println("Error in AdminOrdersListHandler: " + e.getMessage());
                e.printStackTrace();
                HttpUtils.sendError(exchange, 500, e.getMessage());
            }
        }
    }

    // GET /admin/transactions
    public static class AdminTransactionsListHandler implements HttpHandler {
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
                System.out.println("Fetching all transactions..."); // Debug log
                List<Transaction> transactions = transactionService.getAll();
                List<TransactionResponseDto> responseDtos = transactions.stream()
                        .map(TransactionResponseDto::new)
                        .collect(Collectors.toList());
                System.out.println("Transactions fetched: " + responseDtos.size()); // Debug log
                HttpUtils.sendJson(exchange, 200, responseDtos);
            } catch (Exception e) {
                System.err.println("Error in AdminTransactionsListHandler: " + e.getMessage());
                e.printStackTrace();
                HttpUtils.sendError(exchange, 500, e.getMessage());
            }
        }
    }

    // DTO: SuccessResponse
    public static class SuccessResponse {
        private String message;

        public SuccessResponse(String message) {
            this.message = message;
        }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    // LocalDateTimeAdapter
    public static class LocalDateTimeAdapter implements com.google.gson.JsonSerializer<LocalDateTime>, com.google.gson.JsonDeserializer<LocalDateTime> {
        private static final java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public LocalDateTime deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type type, com.google.gson.JsonDeserializationContext context) throws com.google.gson.JsonParseException {
            return LocalDateTime.parse(json.getAsString(), formatter);
        }

        @Override
        public com.google.gson.JsonElement serialize(LocalDateTime src, java.lang.reflect.Type type, com.google.gson.JsonSerializationContext context) {
            return new com.google.gson.JsonPrimitive(formatter.format(src));
        }
    }
}