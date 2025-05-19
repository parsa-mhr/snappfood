package org.example.ServerHandlers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.Main;
import org.example.Security.PasswordUtil;
import org.example.User.Seller;
import org.example.User.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.util.Objects.hash;

public class LoginHandler implements HttpHandler{
   static SessionFactory sessionFactory;
    public LoginHandler(SessionFactory SessionFactory) {
        sessionFactory = SessionFactory;
    }
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (method.equalsIgnoreCase("GET")) {
            loginForm(exchange);
        } else if (method.equalsIgnoreCase("POST")) {
        loginVerify(exchange);
            //to write a method or object for open a panel for user (in case of correct authorize)
        }
    }

    private void loginForm(HttpExchange exchange) throws IOException {
        String response = """
                <!DOCTYPE html>
                <html lang="fa">
                <head>
                    <meta charset="UTF-8">
                    <title>فرم ورود</title>
                    <style>
                        body {
                            font-family: Tahoma, sans-serif;
                            background-color: #f2f2f2;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            height: 100vh;
                        }
                        form {
                            background-color: white;
                            padding: 30px;
                            border-radius: 10px;
                            box-shadow: 0 0 10px rgba(0,0,0,0.1);
                            width: 300px;
                        }
                        input[type="text"], input[type="password"] {
                            width: 100%;
                            padding: 10px;
                            margin: 10px 0;
                            border: 1px solid #ccc;
                            border-radius: 5px;
                        }
                        input[type="submit"] {
                            width: 100%;
                            background-color: #4CAF50;
                            color: white;
                            padding: 10px;
                            border: none;
                            border-radius: 5px;
                            cursor: pointer;
                        }
                        input[type="submit"]:hover {
                            background-color: #45a049;
                        }
                    </style>
                </head>
                <body>
                    <form action="/login" method="post">
                        <h2>ورود به حساب</h2>
                        <label for="identifier">ایمیل یا شماره تماس:</label>
                        <input type="text" id="identifier" name="identifier" required>
                        <label for="password">رمز عبور:</label>
                        <input type="password" id="password" name="password" required>
                        <input type="submit" value="ورود">
                    </form>
                </body>
                </html>
                """;

        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
    private void loginVerify(HttpExchange exchange) throws IOException {
        // خواندن داده‌های ارسال شده از طریق POST
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
        BufferedReader br = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        String formData = sb.toString();
        Map<String, String> params = parseFormData(formData);

        String identifier = params.get("identifier");
        String password = params.get("password");

        User Login = Login (identifier, password);
        if (Login == null) {
            System.out.println("not exist");
        }else {
            System.out.println("login success");
            if (Login instanceof Seller) {
                System.out.println("seller");
            }
        }
    }
    private Map<String, String> parseFormData(String formData) throws UnsupportedEncodingException {
        Map<String, String> map = new HashMap<>();
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                String key = URLDecoder.decode(keyValue[0], "UTF-8");
                String value = URLDecoder.decode(keyValue[1], "UTF-8");
                map.put(key, value);
            }
        }
        return map;
    }
    //login
    public static User Login(String identifier, String password) {
        User user = null;
        String hql ;
        try (Session session = sessionFactory.openSession()) {
            if (identifier.matches("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                System.out.println("Email login");
                System.out.println(identifier);
                 hql = "FROM User s WHERE s.email = :email ";
                user = session.createQuery(hql, User.class)
                        .setParameter("email", identifier)
                        .uniqueResult();
                if (!PasswordUtil.checkPassword(password, user.getPassword()))
                    return null;
                if (user == null) {
                    System.out.println("not exist");
                }
            }else if (identifier.matches("^\\d{10,15}$")) {
                System.out.println("Phone login");
                System.out.println(identifier);
                hql = "FROM User s WHERE s.phonenumber = :phonenumber AND s.password = :password";
                user = session.createQuery(hql, User.class)
                        .setParameter("phonenumber", identifier)
                        .uniqueResult();
                if (!PasswordUtil.checkPassword(password, user.getPassword()))
                    return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return user;
    }

}
