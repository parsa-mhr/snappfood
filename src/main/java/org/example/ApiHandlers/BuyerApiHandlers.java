package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.Details.Cart;
import org.example.Details.CartItem;
import org.example.Details.Coupon;
import org.example.Models.*;
import org.example.Security.jwtSecurity;
import org.example.Services.*;
import org.example.Restaurant.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.User.Buyer;
import org.example.Validation.TokenUserValidator;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static org.example.ApiHandlers.HttpUtils.sendMethodNotAllowed;

public class BuyerApiHandlers {
    private static final Gson gson = new Gson();

    private static final RestaurantService restaurantService = new RestaurantService();
    private static final ItemService itemService = new ItemService();
    private static final OrderService orderService = new OrderService();
    private static final CouponService couponService = new CouponService();
    private static final FavoriteService favoriteService = new FavoriteService();
    private static final RatingService ratingService = new RatingService();
    private static final jwtSecurity jwtSecurity = new jwtSecurity();
    private static SessionFactory sessionFactory ;

    // POST /vendors
    public static class VendorSearchHandler implements HttpHandler {
        private final RestaurantService service = new RestaurantService();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendMethodNotAllowed(exchange);
                return;
            }

            VendorFilter filter;
            try {
                ObjectMapper mapper = new ObjectMapper();
                 filter = mapper.readValue(exchange.getRequestBody(), VendorFilter.class);
            } catch (Exception e) {
                sendError(exchange, 400, "Invalid input");
                return;
            }

            List<Restaurant> vendors = service.findByFilter(filter);
            System.out.println(vendors);
            List<VendorsDto> dtoList = vendors.stream()
                    .map(r -> new VendorsDto(r.getId(), r.getName(), r.getAddress(), r.getPhone() , r.getLogoBase64() , r.getTaxFee() , r.getAdditionalFee()))
                    .toList();
            sendJson(exchange, 200, dtoList);
        }
    }


    // GET /vendors/{id}
    public static class VendorDetailHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) { sendEmpty(exchange,405); return; }
            try {
                int id = parseId(exchange);
                List<MenuItem> opt = restaurantService.getById(id);
                if (opt.size() == 0) { sendError(exchange,404,"Not found"); return; }
                List<MenuItemDto> dtoList = opt.stream()
                        .map(mi -> new MenuItemDto(mi.getId() , mi.getName(), mi.getImageBase64(), mi.getDescription(), Math.toIntExact(mi.getRestaurant().getId()), mi.getPrice(), mi.getSupply()))
                        .toList();
                sendJson(exchange,200,dtoList);
            } catch (NumberFormatException e) {
                sendError(exchange,400,"Invalid ID");
            } catch (Exception e) {
                sendError(exchange,500 , e.getMessage());
            }
        }
    }

    // POST /items (body contains restaurantId filter)
    public static class ItemsListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendEmpty(exchange,405); return; }
            try {
                // expecting {"restaurantId":1}
               ObjectMapper mapper = new ObjectMapper();
               ItemsFilter filter = mapper.readValue(exchange.getRequestBody(), ItemsFilter.class);
                List<MenuItem> items = itemService.findByFilter(filter);
                List<MenuItemDto> list =  items.stream().map(item -> {
                    MenuItemDto dto = new MenuItemDto();
                    dto.id = item.getId();
                    dto.name = item.getName();
                    dto.imageBase64 = (item.getImage() != null)
                            ? Base64.getEncoder().encodeToString(item.getImage())
                            : null;
                    dto.description = item.getDescription();
                    dto.vendor_id = item.getRestaurant().getId().intValue();
                    dto.price = item.getPrice();
                    dto.supply = item.getSupply();
                 //   dto.keywords = item.getKeywords(); // فعلاً category رو جای keywords می‌ذاریم
                    return dto;
                }).toList();
                sendJson(exchange,200,list);
            } catch (JsonSyntaxException|NullPointerException e) {
                sendError(exchange,400,"Invalid payload");
            } catch (Exception e) {
                sendError(exchange,500,e.getMessage());
            }
        }
    }

    // GET /items/{id}
    public static class ItemDetailHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) { sendEmpty(exchange,405); return; }
            try {
                int id = parseId(exchange);
                Optional<MenuItem> opt = itemService.getById(id);
                if (opt.isEmpty()) { sendError(exchange,404,"Not found"); return; }
                MenuItemDto dto = new MenuItemDto();
                MenuItem item = opt.get();
                dto.id = item.getId();
                dto.name = item.getName();
                dto.description = item.getDescription();
                dto.vendor_id = item.getRestaurant().getId().intValue();
                dto.price = item.getPrice();
                dto.supply = item.getSupply();
                dto.imageBase64 = item.getImageBase64();
                sendJson(exchange,200,dto);
            } catch (NumberFormatException e) {
                sendError(exchange,400,"Invalid ID");
            } catch (Exception e) {
                sendError(exchange,500,"Server error");
            }
        }
    }

    // GET /coupons
    public static class CouponsListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendEmpty(exchange, 405);
                return;
            }
            try {
                JsonObject body = gson.fromJson(new InputStreamReader(exchange.getRequestBody()), JsonObject.class);
                String code = body.get("coupon_code").getAsString();

                Optional<Coupon> found = couponService.findByCode(code);
                if (found.isEmpty()) {
                    sendError(exchange, 404, "Coupon not found");
                    return;
                }
                sendJson(exchange, 200, found.get());
            } catch (JsonSyntaxException | NullPointerException e) {
                sendError(exchange, 400, "Invalid payload");
            } catch (Exception e) {
                sendError(exchange, 500, "Server error");
            }
        }
    }

    // POST /orders
    public static class OrdersCreateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendEmpty(exchange, 405);
                return;
            }

            try {
                CreateOrderReq request = gson.fromJson(new InputStreamReader(exchange.getRequestBody()), CreateOrderReq.class);

                // فرض: خریدار (Buyer) از سشن یا توکن خوانده می‌شود:
                String token = exchange.getRequestHeaders().getFirst("Authorization").replace("Bearer " , "");
                TokenUserValidator tokenvalidate = new TokenUserValidator(sessionFactory) ;

                Buyer buyer = (Buyer) tokenvalidate.validate(token); // extract from jwt;
                OrderResponseDto cart = orderService.createOrder(request, buyer);

                sendJson(exchange, 201, cart);
            } catch (JsonSyntaxException e) {
                sendError(exchange, 400, "Invalid JSON payload");
            } catch (IllegalArgumentException e) {
                sendError(exchange, 400, e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Server error");
            }
        }
    }


    // GET /orders/{id}
    public static class OrderDetailHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) { sendEmpty(exchange,405); return; }
            try {
                int id = parseId(exchange);

                OrderResponseDto response = orderService.getById(id);
                if (response ==  null) { sendError(exchange,404,"Not found"); return; }
                sendJson(exchange,200,response);
            } catch (NumberFormatException e) {
                sendError(exchange,400,"Invalid ID");
            } catch (Exception e) {
                sendError(exchange,500,"Server error");
            }
        }
    }



    // GET /orders/history
    public static class OrderHistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) { sendEmpty(exchange,405); return; }
            try {
                //extract from jwt
                long id = (new TokenUserValidator(sessionFactory).validate(exchange.getRequestHeaders().getFirst("Authorization").replace("Bearer " , ""))).getId();
                List<OrderResponseDto> hist = orderService.getHistory((int) id);

//                List<OrderResponseDto> dtos = new ArrayList<>();
//                for (Cart cart : hist) {
//                    String resaurantname= cart.getRestaurant().getName();
//                    Long vendor_id = cart.getRestaurant().getId();
//                    cart.getRestaurant().setSeller(null);
//                    dtos.add(new CartDto(cart , resaurantname, vendor_id));
//                }
                Gson localgson = new GsonBuilder()
                        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                        .create();
                System.out.println(localgson.toJson(hist).toString());
                sendJson(exchange,200, hist);

            } catch (Exception e) {
            e.printStackTrace();          // ← این رو اضافه کنید
            sendError(exchange, 400, e.getMessage());
        }

    }
    }

    // GET /favorites
    public static class FavoritesListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) { sendEmpty(exchange,405); return; }
            try {
                // extract from jwt
                long id = (new TokenUserValidator(sessionFactory).validate(exchange.getRequestHeaders().getFirst("Authorization").replace("Bearer " , ""))).getId();
                List<Favorite> list = favoriteService.listFavorites((int) id);
                for (Favorite favorite : list) {
                    favorite.getRestaurant().setSeller(null);
                }
                sendJson(exchange,200,list);
            } catch (Exception e) {
                sendError(exchange,400,e.getMessage());
            }
        }
    }

    // PUT /favorites/{restaurantID}
    public static class FavoriteAddHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("PUT")) {

                System.out.println("entered");
                try {
                    long id = (new TokenUserValidator(sessionFactory).validate(exchange.getRequestHeaders().getFirst("Authorization").replace("Bearer ", ""))).getId();

                    // 2. استخراج restaurantId از URL
                    String path = exchange.getRequestURI().getPath(); // مثلا "/favorites/42"
                    String[] parts = path.split("/");
                    if (parts.length < 3) {
                        sendError(exchange, 400, "Restaurant ID not found in URL");

                    }
                    int restaurantId = Integer.parseInt(parts[2]);
//عوض شده
                    Restaurant restaurant = restaurantService.getRestaurantById(restaurantId);
                    if (restaurant == null) {
                        sendError(exchange, 404, "Restaurant not found");
                        return;
                    }

                    Favorite fav = new Favorite();
                    fav.setBuyerId((int) id);
                    fav.setRestaurant(restaurant);

                    Favorite saved = favoriteService.addFavorite(fav);
                    saved.getRestaurant().setSeller(null);
                    sendJson(exchange, 201, saved);

                } catch (NumberFormatException e) {
                    sendError(exchange, 400, "Invalid restaurant ID");
                } catch (JsonSyntaxException e) {
                    sendError(exchange, 400, "Invalid payload");
                } catch (Exception e) {
                    sendError(exchange, 500, "Internal server error");
                    e.printStackTrace();
                }
            }

            if (exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
                try {
                    int rid = parseId(exchange);
                    long buyerId = (new TokenUserValidator(sessionFactory).validate(exchange.getRequestHeaders().getFirst("Authorization").replace("Bearer ", ""))).getId();
                    boolean ok = favoriteService.removeFavorite((int) buyerId, rid);
                    sendEmpty(exchange, ok ? 204 : 404);
                } catch (Exception e) {
                    sendError(exchange, 400, "Invalid request");
                }
            }
        }
    }

    // POST /ratings
    public static class RatingsListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendEmpty(exchange,405); return; }
            RatingRequest req = gson.fromJson(
                    new InputStreamReader(exchange.getRequestBody()),
                    RatingRequest.class
            );
            Cart cart = orderService.getCartById(req.getOrder_id());
            if (cart == null) {
                sendError(exchange, 404, "Order not found");
                return;
            }
            List<MenuItem> menuItems = new ArrayList<>();
            Rating rating = new Rating();
            rating.setOrder_id(cart);
            rating.setRating(req.getRating());
            rating.setComment(req.getComment());
            for (CartItem item : cart.getItems()) {
                menuItems.add(item.getMenuItem());
                item.getMenuItem().setKeywords(null);
            }
            rating.setItems(menuItems);
            long BuyerId = 0;
            try {
                BuyerId = (new TokenUserValidator(sessionFactory).validate(exchange.getRequestHeaders().getFirst("Authorization").replace("Bearer ", ""))).getId();

            }catch (Exception e) {
                sendError(exchange, 401, "login again");
            }
            rating.setBuyerId((int) BuyerId);
            if (req.getImageBase64() != null && !req.getImageBase64().isEmpty()) {
                rating.setImageBase64(req.getImageBase64().get(0));
            }
            try {
                Rating rated = ratingService.addRating(rating);
                sendJson(exchange,200,"Rating submitted"+ "Rating id :" + rated.getId());
            }catch (Exception e) {
                sendError(exchange, 400, "Rating submitted on this order before");
            }

        }
    }

    // GET /ratings/items/{item_id}
    public static class RatingsByItemHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) { sendEmpty(exchange,405); return; }
            try {
                int iid = parseId(exchange);
                List<RatingResponseDto> list = ratingService.listByItem(iid);
                sendJson(exchange,200,list);
            } catch (Exception e) {
                sendError(exchange,400,e.getMessage());
            }
        }
    }

    // GET /ratings/{id}
    public static class RatingDetailHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                try {
                    int id = parseId(exchange);
                    RatingResponseDto rating = ratingService.getById(id);
                    if (rating == null) {
                        sendError(exchange, 404, "Rating not found");
                    }else
                    sendJson(exchange, 200, rating);
                } catch (Exception e) {
                    sendError(exchange, 400, e.getMessage());
                }
            }

            //DELETE /ratings/{id}

          else  if (exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {

                boolean success = false;
                try {
                    int id = parseId(exchange);
                    success = ratingService.removeRating(id);
                } catch (Exception e) {
                    sendError(exchange, 400, "Invalid ID");
                }
                if (success) sendJson(exchange, 200, success);
                else HttpUtils.sendJson(exchange, 401, "Resource not found");
            }

            //PUT /ratings/{id}

else if (exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
    RatingRequest body = new RatingRequest();
    try{
    body = gson.fromJson(new InputStreamReader(exchange.getRequestBody()), RatingRequest.class);
}catch (Exception e){
    sendError(exchange, 400, e.getMessage());
    }
    if (body.getRating() == null || body.getComment() == null) {
        sendError(exchange, 400, "Missing required fields");
        return;
    }

    try {
        int id = parseId(exchange);
        Rating rating = ratingService.updateRating(body.getRating(), body.getComment(), body.getImageBase64(), id);

        if (rating == null) {
            sendError(exchange, 404, "Rating not found");
        } else {
            HttpUtils.sendJson(exchange, 200, "rating updated successfuly");
        }
    } catch (Exception e) {
        e.printStackTrace();
        sendError(exchange, 400, "Invalid ID or body");
    }

} else {
    sendError(exchange , 405 , "Invalid request method");
}

        }
    }

            // Helpers
            private static void sendJson (HttpExchange ex,int code, Object obj) throws IOException {
                String j = gson.toJson(obj);
                byte[] b = j.getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                ex.sendResponseHeaders(code, b.length);
                try (OutputStream os = ex.getResponseBody()) {
                    os.write(b);
                }
            }
            private static void sendError (HttpExchange ex,int code, String msg) throws IOException {
                sendJson(ex, code, new ErrorResponse(code, msg));
            }
            private static void sendEmpty (HttpExchange ex,int code) throws IOException {
                ex.sendResponseHeaders(code, -1);
            }
            private static int parseId (HttpExchange ex){
                String[] p = ex.getRequestURI().getPath().split("/");
                return Integer.parseInt(p[p.length - 1]);
            }
        }

