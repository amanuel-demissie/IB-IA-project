package com.javafx.demo.security;

import com.javafx.demo.app.Session;
import com.javafx.demo.model.User;

import java.util.Arrays;

public final class AuthGuard {
    private AuthGuard() {}

    public static boolean isLoggedIn() {
        return Session.getInstance().isLoggedIn();
    }

    public static User currentUser() {
        return Session.getInstance().getCurrentUser();
    }

    public static boolean hasRole(String roleName) {
        User u = currentUser();
        return u != null && roleName != null && roleName.equals(u.roleName());
    }

    public static boolean hasAnyRole(String... roleNames) {
        User u = currentUser();
        if (u == null) return false;
        if (roleNames == null || roleNames.length == 0) return false;
        return Arrays.stream(roleNames).anyMatch(r -> r != null && r.equals(u.roleName()));
    }
}



