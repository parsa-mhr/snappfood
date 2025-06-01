// package org.example.ServerHandlers;

// import com.sun.net.httpserver.HttpExchange;
// import com.sun.net.httpserver.HttpHandler;
// import org.example.Main;
// import org.example.User.Seller;
// import org.example.User.UserRole;

// import java.io.*;
// import java.net.URLDecoder;
// import java.nio.charset.StandardCharsets;
// import java.util.*;

// public class RegisterHandler implements HttpHandler {
//     @Override
//     public void handle(HttpExchange exchange) throws IOException {
//         String method = exchange.getRequestMethod();
//         if (method.equalsIgnoreCase("GET")) {
//             sendForm(exchange);
//         } else if (method.equalsIgnoreCase("POST")) {
//             handleFormSubmission(exchange);
//         }
//     }

//     private void sendForm(HttpExchange exchange) throws IOException {
//         String html = """
//                 <html dir="rtl">
//                 <head>
//                     <meta charset="UTF-8">
//                     <title>ثبت نام</title>
//                     <style>
//                         body {
//                             font-family: 'Vazir', sans-serif;
//                             background: linear-gradient(to right, #8e9eab, #eef2f3);
//                             margin: 0;
//                             display: flex;
//                             align-items: center;
//                             justify-content: center;
//                             height: 100vh;
//                         }
//                         .container {
//                             background-color: white;
//                             padding: 2rem 3rem;
//                             border-radius: 20px;
//                             text-align: center;
//                             box-shadow: 0 8px 30px rgba(0,0,0,0.1);
//                             width: 100%;
//                             max-width: 400px;
//                         }
//                         h2 { color: #333; margin-bottom: 1rem; }
//                         label {
//                             display: block;
//                             text-align: right;
//                             margin-top: 10px;
//                             margin-bottom: 5px;
//                             font-weight: bold;
//                         }
//                         input[type="text"], input[type="file"], input[type="password"] {
//                             width: 100%;
//                             padding: 10px;
//                             border-radius: 5px;
//                             border: 1px solid #ccc;
//                             margin-bottom: 10px;
//                         }
//                         button {
//                             background-color: #007bff;
//                             color: white;
//                             border: none;
//                             padding: 10px 20px;
//                             border-radius: 5px;
//                             cursor: pointer;
//                             font-size: 16px;
//                             width: 100%;
//                         }
//                         button:hover { background-color: #0056b3; }
//                     </style>
//                 </head>
//                 <body>
//                     <div class="container">
//                         <h2>ثبت نام</h2>
//                         <form method="post" enctype="multipart/form-data">
//                             <label>نام و نام خانوادگی:</label>
//                             <input name="fullname" type="text" required /0>
//                             <label>شماره تماس:</label>
//                             <input name="phonenumber" type="text" required /0>
//                             <label>رمز عبور:</label>
//                             <input name="password" type="password" required />
//                             <label>ایمیل:</label>
//                             <input name="email" type="text" required />
//                             <label>نام رستوران:</label>
//                             <input name="shopname" type="text" required />
//                             <label>آپلود تصویر:</label>
//                             <input name="image" type="file" accept="image/*" required />
//                             <label>آدرس:</label>
//                             <input name="adress" type="text" required />
//                             <label>نقش:</label>
//                             <input name="role" type="text" required />
//                             <button type="submit">ارسال کد</button>
//                         </form>
//                     </div>
//                 </body>
//                 </html>
//                 """;

//         exchange.sendResponseHeaders(200, html.getBytes().length);
//         try (OutputStream os = exchange.getResponseBody()) {
//             os.write(html.getBytes());
//         }
//     }

//     private void handleFormSubmission(HttpExchange exchange) throws IOException {
//         String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
//         if (contentType == null || !contentType.contains("multipart/form-data")) {
//             sendError(exchange, "درخواست نامعتبر است.");
//             return;
//         }

//         String boundary = contentType.split("boundary=")[1];
//         byte[] bodyBytes = exchange.getRequestBody().readAllBytes();

//         Map<String, String> fields = new HashMap<>();
//         byte[] imageBytes = null;

//         // Parse multipart form manually
//         String[] parts = new String(bodyBytes, StandardCharsets.ISO_8859_1).split("--" + boundary);
//         for (String part : parts) {
//             if (part.contains("Content-Disposition")) {
//                 String[] headersAndBody = part.split("\r\n\r\n", 2);
//                 if (headersAndBody.length < 2)
//                     continue;

//                 String headers = headersAndBody[0];
//                 String body = headersAndBody[1].trim();

//                 String name = null;
//                 String filename = null;

//                 for (String line : headers.split("\r\n")) {
//                     if (line.contains("Content-Disposition")) {
//                         String[] items = line.split(";");
//                         for (String item : items) {
//                             item = item.trim();
//                             if (item.startsWith("name=")) {
//                                 name = item.substring(6).replaceAll("\"", "");
//                             } else if (item.startsWith("filename=")) {
//                                 filename = item.substring(9).replaceAll("\"", "");
//                             }
//                         }
//                     }
//                 }

//                 if (name == null)
//                     continue;

//                 if (filename != null && !filename.isEmpty()) {
//                     // It's the image file
//                     int startIdx = part.indexOf("\r\n\r\n") + 4;
//                     int endIdx = part.lastIndexOf("\r\n--");
//                     imageBytes = Arrays.copyOfRange(bodyBytes, startIdx, bodyBytes.length - 2);
//                 } else {
//                     fields.put(name, body);
//                 }
//             }
//         }

//         // ساخت شیء Seller
//         Seller seller = new Seller();
//         seller.setFullName(fields.get("fullname"));
//         seller.setPhonenumber(fields.get("phonenumber"));
//         seller.setEmail(fields.get("email"));
//         seller.setPassword(fields.get("password"));
//         seller.setShopName(fields.get("shopname"));
//         seller.setImage(imageBytes);
//         seller.setadress(fields.get("adress"));
//         seller.setRole(UserRole.valueOf(fields.get("role")));
//         System.out.println(seller);
//         Main.inserttodb(seller); // ذخیره در دیتابیس
//         sendSuccess(exchange);
//     }

//     private void sendError(HttpExchange exchange, String msg) throws IOException {
//         exchange.sendResponseHeaders(400, msg.getBytes().length);
//         try (OutputStream os = exchange.getResponseBody()) {
//             os.write(msg.getBytes());
//         }
//     }

//     private void sendSuccess(HttpExchange exchange) throws IOException {
//         String msg = """
//                     <html dir="rtl">
//                     <head>
//                         <meta charset="UTF-8">
//                         <title>ثبت‌نام موفق</title>
//                         <style>
//                             body {
//                                 font-family: "Vazir", sans-serif;
//                                 background: linear-gradient(to right, #8e9eab, #eef2f3);
//                                 display: flex;
//                                 justify-content: center;
//                                 align-items: center;
//                                 height: 100vh;
//                             }
//                             .container {
//                                 background-color: white;
//                                 padding: 2rem;
//                                 border-radius: 20px;
//                                 text-align: center;
//                                 box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
//                                 max-width: 400px;
//                                 width: 100%;
//                             }
//                             h2 {
//                                 color: green;
//                             }
//                         </style>
//                     </head>
//                     <body>
//                         <div class="container">
//                             <h2>ثبت‌نام با موفقیت انجام شد 🎉</h2>
//                         </div>
//                     </body>
//                     </html>
//                 """;
//         exchange.sendResponseHeaders(200, msg.getBytes().length);
//         try (OutputStream os = exchange.getResponseBody()) {
//             os.write(msg.getBytes());
//         }
//     }
// }
