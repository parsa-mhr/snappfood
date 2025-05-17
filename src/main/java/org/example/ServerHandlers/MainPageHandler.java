package org.example.ServerHandlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

 public class MainPageHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
        System.out.println("آی‌پی کاربر: [صفحه اصلی]" + clientIp);
        String response = "<!DOCTYPE html>\n" +
                "<html lang=\"fa\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>صفحه اصلی</title>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            font-family: \"Vazir\", sans-serif;\n" +
                "            background: linear-gradient(to right, #8e9eab, #eef2f3);\n" +
                "            margin: 0;\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "            justify-content: center;\n" +
                "            height: 100vh;\n" +
                "            direction: rtl;\n" +
                "        }\n" +
                "        .main-container {\n" +
                "            background-color: white;\n" +
                "            padding: 2rem 3rem;\n" +
                "            border-radius: 20px;\n" +
                "            text-align: center;\n" +
                "            box-shadow: 0 8px 30px rgba(0,0,0,0.1);\n" +
                "        }\n" +
                "        h1 {\n" +
                "            color: #333;\n" +
                "            margin-bottom: 1rem;\n" +
                "        }\n" +
                "        p {\n" +
                "            color: #555;\n" +
                "            margin-bottom: 2rem;\n" +
                "        }\n" +
                "        a {\n" +
                "            text-decoration: none;\n" +
                "            color: white;\n" +
                "            background-color: #ff4d4f;\n" +
                "            padding: 0.8rem 1.5rem;\n" +
                "            border-radius: 10px;\n" +
                "            font-size: 1.1rem;\n" +
                "            transition: background-color 0.3s ease;\n" +
                "        }\n" +
                "        a:hover {\n" +
                "            background-color: #e60023;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"main-container\">\n" +
                "        <h1>به سایت تست سرور خوش آمدید!</h1>\n" +
                "        <p>لطفاً برای استفاده از امکانات، ثبت\u200Cنام کنید.</p>\n" +
                "        <a href=\"/user\">ثبت\u200Cنام</a>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>\n";

        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");

        exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes("UTF-8"));
        os.close();
    }
}