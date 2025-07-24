package org.example.Validation;

import org.example.Security.PasswordUtil;
import org.example.Unauthorized.UnauthorizedException;
import org.example.User.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * کلاس ExistUser برای اعتبارسنجی کاربر بر اساس شماره تلفن و رمز عبور
 */
public class ExistUser {
    private final SessionFactory sessionFactory;

    /**
     * سازنده کلاس که SessionFactory را دریافت می‌کند
     * @param sessionFactory فکتوری برای ارتباط با پایگاه داده
     */
    public ExistUser(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * اعتبارسنجی کاربر بر اساس شماره تلفن و رمز عبور
     * @param phone شماره تلفن کاربر
     * @param password رمز عبور کاربر
     * @return شیء User در صورت معتبر بودن اطلاعات
     * @throws UnauthorizedException در صورت نامعتبر بودن اطلاعات
     */
    public User validate(String phone, String password) throws UnauthorizedException {
        // بررسی ورودی‌های null یا خالی
        if (phone == null || phone.trim().isEmpty()) {
            throw new UnauthorizedException("شماره تلفن ارائه نشده است", "MISSING_PHONE");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new UnauthorizedException("رمز عبور ارائه نشده است", "MISSING_PASSWORD");
        }

        // اعتبارسنجی فرمت شماره تلفن
//        String phoneRegex = "^(09\\d{9}|۰۹[۰-۹]{9})$";
//        if (!phone.matches(phoneRegex)) {
//            throw new UnauthorizedException("فرمت شماره تلفن نامعتبر است", "INVALID_PHONE");
//        }

        try (Session session = sessionFactory.openSession()) {
            // جستجوی کاربر بر اساس شماره تلفن
            User user = session.createQuery(
                    "FROM User WHERE phonenumber = :phone", User.class)
                    .setParameter("phone", phone)
                    .uniqueResult();

            if (user == null) {
                throw new UnauthorizedException("کاربر با این شماره تلفن یافت نشد", "USER_NOT_FOUND");
            }

            // بررسی رمز عبور
            if (!PasswordUtil.checkPassword(password, user.getPassword())) {
                throw new UnauthorizedException("رمز عبور نادرست است", "INVALID_PASSWORD");
            }

            return user;
        } catch (Exception e) {
            throw new UnauthorizedException("خطای پایگاه داده هنگام اعتبارسنجی کاربر: " + e.getMessage(), "DATABASE_ERROR");
        }
    }
}
