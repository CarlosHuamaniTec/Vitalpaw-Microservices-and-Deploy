package com.vitalpaw.userservice.controller;

import com.vitalpaw.userservice.model.User;
import com.vitalpaw.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        userService.registerUser(user);
        return ResponseEntity.ok("User registered. Check email to verify.");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        String token = userService.loginUser(user.getUsername(), user.getPassword());
        return ResponseEntity.ok(token);
    }

    @GetMapping("/verify/{token}")
    public ResponseEntity<String> verify(@PathVariable String token) {
        userService.verifyUser(token);
        return ResponseEntity.ok("User verified successfully");
    }

    @GetMapping("/all")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }
}