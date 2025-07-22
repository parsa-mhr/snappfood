package org.example;

import org.example.ApiHandlers.*;
import org.example.ApiHandlers.CourierApiHandler;

import org.example.ApiHandlers.BuyerApiHandlers;
import org.example.ApiHandlers.LoginApiHandler;
import org.example.ApiHandlers.LogoutApiHandler;
import org.example.ApiHandlers.ProfileApiHandler;
import org.example.ApiHandlers.RegisterApiHandler;
import org.example.ApiHandlers.RestaurantDispatcher;
import org.example.Details.Cart;
import org.example.Restaurant.MenuItem;
import org.example.Restaurant.Restaurant;

import org.example.Security.PasswordUtil;

import com.sun.net.httpserver.*;
import org.hibernate.*;
import org.hibernate.cfg.*;
import org.example.ServerHandlers.*;
import org.example.User.*;

import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;

import java.util.*;

import static java.lang.Thread.sleep;

public class Main {
    public static SessionFactory sessionFactory;

    public static Seller getSellerBylogin(String email, String password) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM Seller s WHERE s.email = :email";
            Seller seller = session.createQuery(hql, Seller.class)
                    .setParameter("email", email)
                    .uniqueResult();

            if (seller != null && PasswordUtil.checkPassword(password, seller.getPassword())) {
                return seller;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        // connect to db
        Configuration configuration = new Configuration();
        configuration.configure("hibernate.cfg.xml");
        Scanner scanner = new Scanner(System.in);

        // اضافه کردن کلاس‌های Entity
        configuration.addAnnotatedClass(User.class);
        configuration.addAnnotatedClass(Seller.class);
        configuration.addAnnotatedClass(Courier.class);
        configuration.addAnnotatedClass(Buyer.class);
        configuration.addAnnotatedClass(Cart.class);
        configuration.addAnnotatedClass(MenuItem.class);
        configuration.addAnnotatedClass(Restaurant.class);

        sessionFactory = configuration.buildSessionFactory();

        // connect to server
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/auth/register", new RegisterApiHandler(sessionFactory));
        server.createContext("/auth/login", new LoginApiHandler(sessionFactory));
        server.createContext("/vendors", new BuyerApiHandlers.VendorSearchHandler());
        server.createContext("/vendors/", new BuyerApiHandlers.VendorDetailHandler());       // توجه: با slash
        server.createContext("/items", new BuyerApiHandlers.ItemsListHandler());
        server.createContext("/items/", new BuyerApiHandlers.ItemDetailHandler());
        server.createContext("/coupons", new BuyerApiHandlers.CouponsListHandler());
        server.createContext("/orders", new BuyerApiHandlers.OrdersCreateHandler());        // POST
        server.createContext("/orders/", new BuyerApiHandlers.OrderDetailHandler());       // GET
        server.createContext("/orders/history", new BuyerApiHandlers.OrderHistoryHandler());
        server.createContext("/favorites", new BuyerApiHandlers.FavoritesListHandler());
        server.createContext("/favorites/", new BuyerApiHandlers.FavoriteAddHandler());
        server.createContext("/ratings", new BuyerApiHandlers.RatingsListHandler());
        server.createContext("/ratings/items/", new BuyerApiHandlers.RatingsByItemHandler());
        server.createContext("/ratings/", new BuyerApiHandlers.RatingDetailHandler());

        server.createContext("/auth/profile", new ProfileApiHandler(sessionFactory));
        server.createContext("/auth/logout", new LogoutApiHandler());
        server.createContext("/restaurants", new RestaurantDispatcher(sessionFactory));
        server.createContext("/deliveries/available", new CourierApiHandler.AvailableHandler());
        server.createContext("/deliveries/history", new CourierApiHandler.HistoryHandler());
        server.createContext("/deliveries", new CourierApiHandler.UpdateStatusHandler());
//

        server.setExecutor(null);
        server.start();
        System.err.println("Server Started on port " + port + "...!");

    }
}
