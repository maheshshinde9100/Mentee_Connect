package com.mahesh.mentee_connect.security.services;

import com.mahesh.mentee_connect.model.User;
import com.mahesh.mentee_connect.model.Mentor;
import com.mahesh.mentee_connect.model.Student;
import com.mahesh.mentee_connect.model.Admin;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class UserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(UserDetailsImpl.class);

    private String id;
    private String username;
    private String email;
    @JsonIgnore
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private User user;

    public UserDetailsImpl(String id, String username, String email, String password,
            Collection<? extends GrantedAuthority> authorities, User user) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.user = user;
    }

    private static User.UserRole inferRoleFromClass(User user) {
        if (user instanceof Mentor) return User.UserRole.ROLE_MENTOR;
        if (user instanceof Student) return User.UserRole.ROLE_STUDENT;
        if (user instanceof Admin) return User.UserRole.ROLE_ADMIN;
        return null;
    }

    public static UserDetailsImpl build(User user) {
        if (user == null) {
            logger.error("UserDetailsImpl.build called with null User object");
            throw new IllegalArgumentException("Cannot build UserDetails from null User");
        }

        String safeId = user.getId() != null ? user.getId() : "";
        String safeUsername = user.getUsername();
        String safeEmail = user.getEmail();
        String safePassword = user.getPassword() != null ? user.getPassword() : "";

        if (safeUsername == null || safeUsername.trim().isEmpty()) {
            safeUsername = safeEmail != null ? safeEmail : "";
            if (safeId != null && !safeId.isEmpty()) {
                logger.warn("User id={} has empty username field, falling back to email='{}'", safeId, safeUsername);
            }
        }
        if (safeEmail == null || safeEmail.trim().isEmpty()) {
            safeEmail = safeUsername;
            if (safeId != null && !safeId.isEmpty()) {
                logger.warn("User id={} has empty email field, falling back to username='{}'", safeId, safeEmail);
            }
        }

        User.UserRole role = user.getRole();
        List<GrantedAuthority> authorities;
        if (role == null) {
            User.UserRole inferred = inferRoleFromClass(user);
            if (inferred != null) {
                logger.warn("User id={} has null role in DB, inferred role={} from entity class {} and applying default.",
                        safeId, inferred, user.getClass().getSimpleName());
                role = inferred;
                user.setRole(inferred);
            } else {
                logger.error("User id={} has null role and cannot infer from class {} — returning empty authorities list.",
                        safeId, user.getClass().getName());
                authorities = new ArrayList<>();
                return new UserDetailsImpl(safeId, safeUsername, safeEmail, safePassword, authorities, user);
            }
        }

        authorities = List.of(new SimpleGrantedAuthority(role.name()));
        return new UserDetailsImpl(safeId, safeUsername, safeEmail, safePassword, authorities, user);
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username != null && !username.trim().isEmpty() ? username : email;
    }

    public String getRawUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
