package org.example;

import org.example.Details.Cart;
import org.example.Details.OrderStatus;
import org.example.Security.PasswordUtil;//
import com.sun.net.httpserver.*;
import org.hibernate.*;
import org.hibernate.cfg.*;
import org.example.ServerHandlers.*;
import org.example.User.*;
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
        if (user instanceof Courier) {
            Courier courier = (Courier) user;
            if (!courier.getbankinformation().matches(cardNumberRegex)) {
                System.out.println("Invalid card number format: " + courier.getbankinformation());
                return;
            }
        }

        // اعتبارسنجی شماره تماس
        String phoneRegex = "^(09\\d{9}|۰۹[۰-۹]{9})$";
        if (!user.getphonenumber().matches(phoneRegex)) {
            System.out.println("Invalid phone number format: " + user.getphonenumber());
            return; // شماره نامعتبر است، ذخیره انجام نمی‌شود
        }
        String emailRegex = "^[a-zA-Z0-9._%+-]+@gmail\\.com$";
        if (!user.getEmail().matches(emailRegex)) {
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
                    .setParameter("phonenumber", user.getphonenumber())
                    .uniqueResult();

            if (existingUserByPhone != null) {
                System.out.println("User with phone number " + user.getphonenumber() + " already exists.");
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
        sessionFactory = configuration.buildSessionFactory();

        // connect to server
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new MainPageHandler());
        server.createContext("/user", new RegisterHandler());
        server.createContext("/login", new LoginHandler(sessionFactory));
        server.setExecutor(null);
        server.start();
        System.err.println("Server Started on port " + port + "...!");
        //test
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            Buyer buyer = new Buyer("name" , "last"  , "mail@gmail.com" , "1234" , "09124357677");
            Cart cart = new Cart(buyer);
            session.save(buyer);
            session.save(cart);
            transaction.commit();
            sleep(10000);
            System.out.println("Cart updated" );
            cart.setStatus(OrderStatus.Deliverd , sessionFactory);

        }
    }
}
