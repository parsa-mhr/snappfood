package org.example;

import org.example.ApiHandlers.BuyerApiHandlers;
import org.example.ApiHandlers.BuyerApiHandlers;
import org.example.ApiHandlers.LoginApiHandler;
import org.example.ApiHandlers.ProfileApiHandler;
import org.example.ApiHandlers.RegisterApiHandler;
import org.example.Details.Cart;
import org.example.Details.OrderStatus;
import org.example.Security.PasswordUtil;
import org.example.Restaurant.MenuItem;
import org.example.Restaurant.Restaurant;
import org.example.Security.PasswordUtil;//
import com.sun.net.httpserver.*;
import org.hibernate.*;
import org.hibernate.cfg.*;
import org.example.ServerHandlers.*;
import org.example.User.*;
import org.example.ApiHandlers.BuyerApiHandlers;
import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.lang.Thread.sleep;

public class Main {
    public static SessionFactory sessionFactory;

    public static void inserttodb(User user) {
        String cardNumberRegex = "^\\d{16}$";

        if (user instanceof Courier courier) {
            BankInfo bankInfo = courier.getBankInformation();
            if (bankInfo == null || bankInfo.getAccountNumber() == null ||
                    !bankInfo.getAccountNumber().matches(cardNumberRegex)) {
                System.out.println("Invalid card number format: " +
                        (bankInfo != null ? bankInfo.getAccountNumber() : "null"));
                return;
            }
        }

        // اعتبارسنجی شماره تماس
        String phoneRegex = "^(09\\d{9}|۰۹[۰-۹]{9})$";
        if (!user.getPhonenumber().matches(phoneRegex)) {
            System.out.println("Invalid phone number format: " + user.getPhonenumber());
            return; // شماره نامعتبر است، ذخیره انجام نمی‌شود
        }
        String regexp = "^[\\w\\.-]+@([\\w-]+\\.)+[A-Za-z]{2,}$";
        if (!user.getEmail().matches(regexp)) {
            System.out.println("Invalid email format: " + user.getEmail());
            return;
        }

        Session session = sessionFactory.openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();

            // بررسی وجود کاربر با ایمیل مشابه
            String emailHql = "FROM User u WHERE u.email = :email";
            User existingUserByEmail = session.createQuery(emailHql, User.class)
                    .setParameter("email", user.getEmail())
                    .uniqueResult();

            if (existingUserByEmail != null) {
                System.out.println("User with email " + user.getEmail() + " already exists.");
                return;
            }

            // بررسی وجود کاربر با شماره تماس مشابه
            String phoneHql = "FROM User u WHERE u.phonenumber = :phonenumber";
            User existingUserByPhone = session.createQuery(phoneHql, User.class)
                    .setParameter("phonenumber", user.getPhonenumber())
                    .uniqueResult();

            if (existingUserByPhone != null) {
                System.out.println("User with phone number " + user.getPhonenumber() + " already exists.");
                return;
            }
            user.setPassword(PasswordUtil.hashPassword(user.getPassword()));// هش کردن پسورد
            session.save(user);
            transaction.commit();
            System.out.println("User saved with ID: " + user.getId());
        } catch (Exception e) {
            if (transaction != null)
                transaction.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

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
        server.createContext("/ratings", new BuyerApiHandlers.RatingsListHandler());
        server.createContext("/ratings/items/", new BuyerApiHandlers.RatingsByItemHandler());
        server.createContext("/ratings/", new BuyerApiHandlers.RatingDetailHandler());

        server.createContext("/auth/profile", new ProfileApiHandler(sessionFactory));
        server.createContext("/auth/logout", new LoginApiHandler(sessionFactory));

        server.setExecutor(null);
        server.start();
        System.err.println("Server Started on port " + port + "...!");
    }
}
