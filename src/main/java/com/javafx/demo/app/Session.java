package com.javafx.demo.app;

import com.javafx.demo.model.User;

public final class Session {
    private static final Session INSTANCE = new Session();

    private User currentUser;

    private Session() {}

    public static Session getInstance() {
        return INSTANCE;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}


