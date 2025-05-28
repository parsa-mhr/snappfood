package org.example.Validation;

import org.example.AlredyExist.AlredyExistException;
import org.example.User.*;
import org.example.invalidFieldName.InvalidFieldException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.Base64;
import java.util.List;

public class CheckUser {

    private final SessionFactory sessionFactory;

    public CheckUser(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void validate(User user, String profileImageBase64) {
        // 1. فیلدهای عمومی
        if (user.getFullName() == null)
            throw new InvalidFieldException("FullName");
        if (user.getPassword() == null)
            throw new InvalidFieldException("Password");
        if (user.getPhonenumber() == null)
            throw new InvalidFieldException("Phone");
        if (user.getadress() == null)
            throw new InvalidFieldException("Address");
        if (user.getRole() == null)
            throw new InvalidFieldException("Role");

        // 2. اعتبارسنجی نقش
        List<UserRole> validRoles = List.of(UserRole.buyer, UserRole.seller, UserRole.courier);
        if (!validRoles.contains(user.getRole())) {
            throw new InvalidFieldException("Role");
        }

        // 3. اعتبارسنجی base64 عکس اگر وجود داشت
        if (profileImageBase64 != null) {
            try {
                Base64.getDecoder().decode(profileImageBase64);
            } catch (Exception e) {
                throw new InvalidFieldException("ProfileImageBase64");
            }
        }

        // 4. اعتبارسنجی شماره تماس
        String phoneRegex = "^(09\\d{9}|۰۹[۰-۹]{9})$";
        if (!user.getPhonenumber().matches(phoneRegex)) {
            throw new InvalidFieldException("Phone Number");
        }

        // 5. اعتبارسنجی ایمیل (اگر مقدار داشته باشه)
        if (user.getEmail() != null &&
                !user.getEmail().matches("^[\\w\\.-]+@([\\w-]+\\.)+[A-Za-z]{2,}$")) {
            throw new InvalidFieldException("Email");
        }

        // 6. اعتبارسنجی شماره کارت (اگر Courier و BankInfo داده شده)
        if (user instanceof Courier courier && courier.getBankInformation() != null) {
            BankInfo bank = courier.getBankInformation();
            if (bank.getAccountNumber() != null) {
                String cardRegex = "^\\d{16}$";
                if (!bank.getAccountNumber().matches(cardRegex)) {
                    throw new InvalidFieldException("Account Number");
                }
            }
        }

        // 7. بررسی تکراری بودن ایمیل و شماره در دیتابیس
        try (Session session = sessionFactory.openSession()) {
            if (user.getEmail() != null) {
                User existingByEmail = session.createQuery(
                        "FROM User WHERE email = :email", User.class)
                        .setParameter("email", user.getEmail())
                        .uniqueResult();
                if (existingByEmail != null) {
                    throw new AlredyExistException("Email");
                }
            }

            User existingByPhone = session.createQuery(
                    "FROM User WHERE phonenumber = :phone", User.class)
                    .setParameter("phone", user.getPhonenumber())
                    .uniqueResult();
            if (existingByPhone != null) {
                throw new AlredyExistException("Phone Number");
            }
        }
    }

}
