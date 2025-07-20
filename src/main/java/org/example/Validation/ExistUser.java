package org.example.Validation;

import org.example.Security.PasswordUtil;
import org.example.Unauthorized.UnauthorizedException;
import org.example.User.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class ExistUser {
    private final SessionFactory sessionFactory;

    public ExistUser(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public User validate(String phone, String password) throws UnauthorizedException {
        if (phone == null || password == null)
            throw new UnauthorizedException();

        String phoneRegex = "^(09\\d{9}|۰۹[۰-۹]{9})$";
        if (!phone.matches(phoneRegex))
            throw new UnauthorizedException();

        try (Session session = sessionFactory.openSession()) {
            User user = session.createQuery(
                    "FROM User WHERE phonenumber = :phone", User.class)
                    .setParameter("phone", phone)
                    .uniqueResult();

            if (user == null)
                throw new UnauthorizedException();

            if (!PasswordUtil.checkPassword(password, user.getPassword()))
                throw new UnauthorizedException();

            return user;
        }
    }
}
