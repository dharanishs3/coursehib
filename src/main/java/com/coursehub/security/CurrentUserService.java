package com.coursehub.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.coursehub.entity.User;
import com.coursehub.entity.enums.UserRole;

/**
 Tells services who is logged in, using the user stored in the SecurityContext by the JWT filter.
 Services call this instead of accepting a userId from the request, so a client cannot pretend to be someone else.
 */
@Service
public class CurrentUserService {

    private CustomUserDetails getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (CustomUserDetails) authentication.getPrincipal();
    }

    public Long getCurrentUserId() {
        return getPrincipal().getUserId();
    }

    public UserRole getCurrentRole() {
        return getPrincipal().getUser().getRole();
    }

    public User getCurrentUser() {
        return getPrincipal().getUser();
    }
}
