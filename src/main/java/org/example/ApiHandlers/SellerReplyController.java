package org.example.ApiHandlers;

import com.sun.net.httpserver.HttpHandler;

import java.io.OutputStream;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.Models.Rating;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class SellerReplyController {
    //POST ratings/{id}/reply


    public static class RatingReplyHandler implements HttpHandler {

        private final SessionFactory sessionFactory;

        public RatingReplyHandler(SessionFactory sessionFactory) {
            this.sessionFactory = sessionFactory;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                return;
            }

            String path = exchange.getRequestURI().getPath(); // e.g. /ratings/5/reply
            String[] parts = path.split("/");
            if (parts.length < 4) {
                exchange.sendResponseHeaders(400, -1); // Bad Request
                return;
            }

            Long ratingId;
            try {
                ratingId = Long.parseLong(parts[3]);
            } catch (NumberFormatException e) {
                exchange.sendResponseHeaders(400, -1); // Bad Request
                return;
            }

            // خواندن message از بدنه درخواست
            BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
            StringBuilder bodyBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                bodyBuilder.append(line);
            }

            String message;
            try {
                JSONObject json = new JSONObject(bodyBuilder.toString());
                message = json.getString("message");
            } catch (Exception e) {
                exchange.sendResponseHeaders(400, -1); // Bad JSON
                return;
            }

            // Hibernate Session
            Session session = sessionFactory.openSession();
            Transaction tx = null;

            try {
                tx = session.beginTransaction();

                // پیدا کردن Rating با HQL
                Rating rating = session.get(Rating.class, ratingId);
                if (rating == null) {
                    exchange.sendResponseHeaders(404, -1); // Not Found
                    return;
                }

                // آپدیت فیلد sellerReply
                rating.setSellerReply(message);
                session.update(rating);

                tx.commit();

                String response = "Reply added successfully.";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } catch (Exception e) {
                if (tx != null) tx.rollback();
                exchange.sendResponseHeaders(500, -1); // Server Error
            } finally {
                session.close();
            }
        }
    }

}
