package com.aclc.web.service;

import com.aclc.web.model.User;
import com.aclc.web.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public boolean register(String username, String email, String password) {
        if (userRepo.existsByUsername(username)) return false;
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(password));   // ✅ matches User.setPasswordHash()
        userRepo.save(u);
        return true;
    }

    public User login(String usernameOrEmail, String password) {
        Optional<User> opt = userRepo.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
        if (opt.isPresent() && encoder.matches(password, opt.get().getPasswordHash())) {
            return opt.get();
        }
        return null;
    }
}