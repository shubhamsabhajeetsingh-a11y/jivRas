package com.jivRas.groceries.controller;

import com.jivRas.groceries.config.JwtService;
import com.jivRas.groceries.entity.User;
import com.jivRas.groceries.kaafka.KafkaEventProducer;
import com.jivRas.groceries.repository.UserRepository;

import lombok.NoArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200")

public class UserController {

    private final DaoAuthenticationProvider authenticationProvider;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
	private final KafkaEventProducer kafkaEventProducer;
	private final JwtService jwtService;
	
   
    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager, KafkaEventProducer kafkaEventProducer, JwtService jwtService, DaoAuthenticationProvider authenticationProvider) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.kafkaEventProducer = kafkaEventProducer;
		this.jwtService = jwtService;
		this.authenticationProvider = authenticationProvider;
	}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {

        try {
            String username = credentials.get("username");
            String password = credentials.get("password");

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            if (authentication.isAuthenticated()) {

                String token = jwtService.generateToken(username);
                System.out.println(token);
//                kafkaEventProducer.sendLoginEvent(username);
                return ResponseEntity.ok(
                        Map.of("token", token)
                );
            }
            return ResponseEntity.status(401).body("Invalid credentials");

        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body("Invalid credentials from authentication");
        }
    }
    
    
    @PostMapping("/create")
    public ResponseEntity<?> createUser(@RequestBody User user) {
            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
                return ResponseEntity
                        .badRequest()
                        .body("User already exists: " + user.getUsername());
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return ResponseEntity.ok("Users created successfully");
    }
    
    @PostMapping("/createMultiUser")
    public ResponseEntity<?> createMultipleUsers(@RequestBody List<User> users) {

        List<String> existingUsers = new ArrayList<>();
        List<User> usersToSave = new ArrayList<>();

        for (User user : users) {

            Optional<User> existing = userRepository.findByUsername(user.getUsername());

            if (existing.isPresent()) {
                existingUsers.add(user.getUsername());
            } else {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
                usersToSave.add(user);
            }
        }

        if (!usersToSave.isEmpty()) {
            userRepository.saveAll(usersToSave);
        }

        if (!existingUsers.isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body("These users already exist: " + existingUsers);
        }

        return ResponseEntity.ok("Users created successfully");
    }
}