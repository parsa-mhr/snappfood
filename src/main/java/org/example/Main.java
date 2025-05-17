package org.example;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.* ;
import org.example.User.* ;
public class Main {
    public static void main(String[] args) {
        // کانفیگ Hibernate از فایل hibernate.cfg.xml می‌خوانیم
        Configuration configuration = new Configuration();
        configuration.configure("hibernate.cfg.xml");

        // اضافه کردن کلاس‌های Entity
        configuration.addAnnotatedClass(User.class);
        configuration.addAnnotatedClass(Seller.class);

        SessionFactory sessionFactory = configuration.buildSessionFactory();
        Session session = sessionFactory.openSession();

        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();

            // ساخت شی Seller
            Seller seller = new Seller("Koskhold", "MyShop@gmail.com", "123" , "hasanshop");
            // ذخیره شی
            seller.setShopName("HassanShop");
            session.save(seller);

            transaction.commit();
            System.out.println("Seller saved with ID: " + seller.getId());
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        } finally {
            session.close();
            sessionFactory.close();
        }
    }
}
