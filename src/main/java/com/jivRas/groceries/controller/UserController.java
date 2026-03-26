package com.jivRas.groceries.controller;

import java.security.Principal;
import java.sql.Date;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jivRas.groceries.dto.UserProfileDTO;

import com.jivRas.groceries.config.JwtService;
import com.jivRas.groceries.dto.CreateCustomerRequest;
import com.jivRas.groceries.dto.CreateEmployeeRequest;
import com.jivRas.groceries.entity.Customer;
import com.jivRas.groceries.entity.EmployeeUser;
import com.jivRas.groceries.entity.RefreshToken;
import com.jivRas.groceries.kaafka.KafkaEventProducer;
import com.jivRas.groceries.repository.CustomerRepository;
import com.jivRas.groceries.repository.EmployeeUserRepository;
import com.jivRas.groceries.repository.RefreshTokenRepository;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final CustomerRepository customerRepository;
    private final EmployeeUserRepository employeeUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final KafkaEventProducer kafkaEventProducer;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserController(CustomerRepository customerRepository, EmployeeUserRepository employeeUserRepository,
            PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
            KafkaEventProducer kafkaEventProducer, JwtService jwtService,
            DaoAuthenticationProvider authenticationProvider, RefreshTokenRepository refreshTokenRepository) {
        this.customerRepository = customerRepository;
        this.employeeUserRepository = employeeUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.kafkaEventProducer = kafkaEventProducer;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    private boolean usernameExists(String username) {
        return customerRepository.findByUsernameAndAccountCreatedTrue(username).isPresent() ||
               employeeUserRepository.findByUsername(username).isPresent();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {

        String username = credentials.get("username");
        String password = credentials.get("password");

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            if (authentication.isAuthenticated()) {

                UserDetails userDetails = (UserDetails) authentication.getPrincipal();

                String role = userDetails.getAuthorities()
                        .iterator()
                        .next()
                        .getAuthority();

                // Trigger login audit via Kafka
                kafkaEventProducer.sendLoginEvent(username, role);

                String accessToken = jwtService.generateAccessToken(username, role);
                String refreshToken = jwtService.generateRefreshToken(username);

                RefreshToken token = new RefreshToken();
                token.setUsername(username);
                token.setToken(refreshToken);
                token.setExpiryDate(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7));

                refreshTokenRepository.save(token);

                return ResponseEntity.ok(Map.of(
                        "accessToken", accessToken,
                        "refreshToken", refreshToken,
                        "role", role
                ));
            }
        } catch (AuthenticationException e) {
            System.out.println("Login Failed: " + e.getMessage());
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        return ResponseEntity.status(401).body("Invalid credentials");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        
        // Remove refresh token
        refreshTokenRepository.deleteByUsername(username);

        // Trigger logout audit via Kafka
        kafkaEventProducer.sendLogoutEvent(username);

        return ResponseEntity.ok("Logged out successfully");
    }

    /**
     * Public endpoint — customer self-registration.
     * Uses Customer table and sets isAccountCreated = true.
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerCustomer(@RequestBody CreateCustomerRequest request) {

        if (usernameExists(request.getUsername())) {
            return ResponseEntity.badRequest().body("Username already exists: " + request.getUsername());
        }

        Customer customer = new Customer();
        customer.setName(request.getFirstName() + " " + request.getLastName());
        customer.setMobile(request.getMobile());
        customer.setAddressLine(request.getAddress());
        // For email, we don't have it on Customer entity, we can just skip or add if needed, 
        // but it's optional in CreateCustomerRequest anyway.
        
        customer.setUsername(request.getUsername());
        customer.setPassword(passwordEncoder.encode(request.getPassword()));
        customer.setRole("CUSTOMER");
        customer.setAccountCreated(true); // Must use standard lombok setter for boolean isAccountCreated

        customerRepository.save(customer);
        return ResponseEntity.ok("Customer registered successfully");
    }

    // Made public so an existing admin is no longer required
    @PostMapping("/register-employee")
    public ResponseEntity<?> registerEmployee(@RequestBody CreateEmployeeRequest request, Principal principal) {
        if (usernameExists(request.getUsername())) {
            return ResponseEntity.badRequest().body("Username already exists: " + request.getUsername());
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body("Email is mandatory for employee registration");
        }

        EmployeeUser user = new EmployeeUser();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setMobile(request.getMobile());
        user.setAddress(request.getAddress());
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        
        // If an admin isn't logged in, log "Self-Registered"
        String creator = (principal != null) ? principal.getName() : "Self-Registered";
        user.setCreatedBy(creator);

        employeeUserRepository.save(user);
        return ResponseEntity.ok("Employee registered successfully by " + creator);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        String username = principal.getName();
        
        Optional<EmployeeUser> employeeOpt = employeeUserRepository.findByUsername(username);
        if (employeeOpt.isPresent()) {
            EmployeeUser emp = employeeOpt.get();
            return ResponseEntity.ok(new UserProfileDTO(emp.getFirstName(), emp.getLastName(), emp.getAddress(), emp.getRole()));
        }
        
        Optional<Customer> customerOpt = customerRepository.findByUsernameAndAccountCreatedTrue(username);
        if (customerOpt.isPresent()) {
            Customer cust = customerOpt.get();
            String[] names = cust.getName() != null ? cust.getName().split(" ", 2) : new String[]{"", ""};
            String firstName = names[0];
            String lastName = names.length > 1 ? names[1] : "";
            return ResponseEntity.ok(new UserProfileDTO(firstName, lastName, cust.getAddressLine(), cust.getRole()));
        }
        
        return ResponseEntity.notFound().build();
    }
}