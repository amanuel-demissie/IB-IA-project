package com.javafx.demo.security;

import com.javafx.demo.dao.UserDao;
import com.javafx.demo.model.User;

public class AuthService {
    private final UserDao userDao = new UserDao();

    public User login(String username, String password) {
        User user = userDao.findByUsername(username);
        if (user != null && PasswordHasher.matches(password, user.passwordHash())) {
            return user;
        }
        return null;
    }

    public void seedAdminIfMissing() {
        // For development: default admin/admin123. In production, prompt or secure differently.
        String defaultAdminUsername = "admin";
        String defaultAdminPasswordHash = PasswordHasher.hash("admin123");
        userDao.ensureAdminSeeded(defaultAdminUsername, defaultAdminPasswordHash);
    }
}


