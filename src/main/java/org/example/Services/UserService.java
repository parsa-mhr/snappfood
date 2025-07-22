package org.example.Services;

import org.example.DAO.UserDAO;

import org.example.Models.UserStatusRequest;
import org.example.User.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import java.util.List;

public class UserService {
    private final UserDAO userDAO;

    public UserService(SessionFactory sessionFactory) {
        this.userDAO = new UserDAO(sessionFactory);
    }

    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    public void updateUserStatus(Long userId, UserStatusRequest request) {
        try (Session session = userDAO.sessionFactory.openSession()) {
            session.beginTransaction();
            User user = userDAO.findById(userId);
            if (user == null) {
                throw new RuntimeException("User not found: " + userId);
            }
            if (!request.getStatus().equals("approved") && !request.getStatus().equals("rejected")) {
                throw new IllegalArgumentException("Invalid status");
            }
            user.setStatus(request.getStatus());
            userDAO.update(user);
            session.getTransaction().commit();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update user status: " + e.getMessage(), e);
        }
    }
}