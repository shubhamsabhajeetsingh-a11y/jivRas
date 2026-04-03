package com.jivRas.groceries.controller;

import java.security.Principal;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jivRas.groceries.service.DynamicAuthorizationService;

import jakarta.servlet.http.HttpServletRequest;

import com.jivRas.groceries.config.JwtService;
import com.jivRas.groceries.dto.CreateCustomerRequest;
import com.jivRas.groceries.dto.CreateEmployeeRequest;
import com.jivRas.groceries.dto.EmployeeDetailResponse;
import com.jivRas.groceries.dto.UserProfileDTO;
import com.jivRas.groceries.entity.Branch;
import com.jivRas.groceries.entity.Customer;
import com.jivRas.groceries.entity.EmployeeUser;
import com.jivRas.groceries.entity.RefreshToken;
import com.jivRas.groceries.kafka.KafkaEventProducer;
import com.jivRas.groceries.repository.BranchRepository;
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
    private final DynamicAuthorizationService dynamicAuthorizationService;
    private final BranchRepository branchRepository;

    public UserController(CustomerRepository customerRepository, EmployeeUserRepository employeeUserRepository,
            PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
            KafkaEventProducer kafkaEventProducer, JwtService jwtService,
            DaoAuthenticationProvider authenticationProvider, RefreshTokenRepository refreshTokenRepository,
            DynamicAuthorizationService dynamicAuthorizationService,
            BranchRepository branchRepository) {
        this.customerRepository = customerRepository;
        this.employeeUserRepository = employeeUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.kafkaEventProducer = kafkaEventProducer;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.dynamicAuthorizationService = dynamicAuthorizationService;
        this.branchRepository = branchRepository;
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

                String roleRaw = userDetails.getAuthorities()
                        .iterator()
                        .next()
                        .getAuthority();

                // Strip ROLE_ prefix added by Spring's .roles() helper
                // DB stores "ADMIN", JWT and frontend expect "ADMIN" (not "ROLE_ADMIN")
                String role = roleRaw.startsWith("ROLE_") ? roleRaw.substring(5) : roleRaw;

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

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String requestRefreshToken = request.get("refreshToken");
        if (requestRefreshToken == null || requestRefreshToken.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Refresh token is required");
        }

        return refreshTokenRepository.findByToken(requestRefreshToken)
                .map(token -> {
                    if (token.getExpiryDate().before(new java.util.Date())) {
                        refreshTokenRepository.delete(token);
                        return ResponseEntity.status(401).body("Refresh token has expired");
                    }
                    
                    String username = token.getUsername();
                    String role = "";
                    Optional<EmployeeUser> employeeOpt = employeeUserRepository.findByUsername(username);
                    if(employeeOpt.isPresent()) {
                    	role = employeeOpt.get().getRole();
                    } else {
                    	Optional<Customer> customerOpt = customerRepository.findByUsernameAndAccountCreatedTrue(username);
                    	if(customerOpt.isPresent()) {
                    		role = customerOpt.get().getRole();
                    	}
                    }
                    
                    if(role.isEmpty()) {
                    	return ResponseEntity.status(401).body("User not found");
                    }

                    String newAccessToken = jwtService.generateAccessToken(username, role);
                    return ResponseEntity.ok(Map.of(
                            "accessToken", newAccessToken,
                            "refreshToken", requestRefreshToken,
                            "role", role
                    ));
                })
                .orElseGet(() -> ResponseEntity.status(401).body("Refresh token not found"));
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
     
        // Validate that EMPLOYEE and BRANCH_MANAGER must have a branchId
        // ADMIN role doesn't need one (they see all branches)
        if (!request.getRole().equals("ADMIN") && request.getBranchId() == null) {
            return ResponseEntity.badRequest().body("branchId is required for EMPLOYEE and BRANCH_MANAGER roles");
        }
     
        EmployeeUser user = new EmployeeUser();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setMobile(request.getMobile());
        user.setAddress(request.getAddress());
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
     
        // Set role — "EMPLOYEE", "BRANCH_MANAGER", or "ADMIN"
        user.setRole(request.getRole());
     
        // NEW: Assign branchId — links this employee to their branch permanently
        user.setBranchId(request.getBranchId());
     
        // Set who created this account (the logged-in admin/manager)
        user.setCreatedBy(principal != null ? principal.getName() : "SYSTEM");
     
        employeeUserRepository.save(user);
        return ResponseEntity.ok("Employee registered successfully");
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
    
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Principal principal) {

        // Principal is null when no valid JWT is present
        if (principal == null) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        String username = principal.getName();

        // Try to find in EmployeeUser table
        Optional<EmployeeUser> employeeOpt = employeeUserRepository.findByUsername(username);
        if (employeeOpt.isPresent()) {
            EmployeeUser emp = employeeOpt.get();

            // Return profile with branchId — Angular uses this for dashboard routing
            // branchId is null for ADMIN (they see all branches via dropdown)
            return ResponseEntity.ok(Map.of(
                    "username", emp.getUsername(),
                    "role", emp.getRole(),
                    "firstName", emp.getFirstName() != null ? emp.getFirstName() : "",
                    "lastName", emp.getLastName() != null ? emp.getLastName() : "",
                    "email", emp.getEmail() != null ? emp.getEmail() : "",
                    "branchId", emp.getBranchId() != null ? emp.getBranchId() : 0
            ));
        }

        return ResponseEntity.status(404).body("Profile not found");
    }

    /**
     * GET /api/users/roles
     * ADMIN-only: returns distinct assignable roles from the EmployeeUser table,
     * excluding ADMIN (which cannot be assigned via the Create Role form).
     */
    @GetMapping("/roles")
    public ResponseEntity<?> getAssignableRoles(
            HttpServletRequest httpRequest,
            Authentication authentication) {

        String role = resolveRole(authentication);
        if (!dynamicAuthorizationService.isAllowed(role, httpRequest.getRequestURI(), httpRequest.getMethod())) {
            return ResponseEntity.status(403).body("Access denied");
        }

        List<String> roles = employeeUserRepository.findDistinctRolesExcludingAdmin();
        System.out.println("Roles fetched from DB: " + roles);
        return ResponseEntity.ok(roles);
    }

    // ── Role Definition tab APIs ─────────────────────────────────────────────

    /**
     * GET /api/users/employees
     * ADMIN only — returns all employees (excluding ADMIN) enriched with branchName.
     */
    @GetMapping("/employees")
    public ResponseEntity<?> getAllEmployees(
            HttpServletRequest httpRequest,
            Authentication authentication) {

        String role = resolveRole(authentication);
        if (!dynamicAuthorizationService.isAllowed(role, httpRequest.getRequestURI(), httpRequest.getMethod())) {
            return ResponseEntity.status(403).body("Access denied");
        }

        List<EmployeeUser> employees = employeeUserRepository.findAllByRoleNot("ADMIN");
        List<EmployeeDetailResponse> result = employees.stream().map(emp -> {
            String branchName = "—";
            if (emp.getBranchId() != null) {
                Optional<Branch> branch = branchRepository.findById(emp.getBranchId());
                branchName = branch.map(Branch::getName).orElse("Unknown");
            }
            return EmployeeDetailResponse.builder()
                    .id(emp.getId())
                    .firstName(emp.getFirstName())
                    .lastName(emp.getLastName())
                    .email(emp.getEmail())
                    .mobile(emp.getMobile())
                    .address(emp.getAddress())
                    .username(emp.getUsername())
                    .role(emp.getRole())
                    .branchId(emp.getBranchId())
                    .branchName(branchName)
                    .build();
        }).toList();

        return ResponseEntity.ok(result);
    }

    /**
     * PUT /api/users/employees/{id}
     * ADMIN only — update mobile and address of an employee.
     */
    @PutMapping("/employees/{id}")
    public ResponseEntity<?> updateEmployee(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        String role = resolveRole(authentication);
        if (!dynamicAuthorizationService.isAllowed(role, httpRequest.getRequestURI(), httpRequest.getMethod())) {
            return ResponseEntity.status(403).body("Access denied");
        }

        return employeeUserRepository.findById(id).map(emp -> {
            if (body.containsKey("mobile"))  emp.setMobile(body.get("mobile"));
            if (body.containsKey("address")) emp.setAddress(body.get("address"));
            EmployeeUser saved = employeeUserRepository.save(emp);

            String branchName = "—";
            if (saved.getBranchId() != null) {
                branchName = branchRepository.findById(saved.getBranchId())
                        .map(Branch::getName).orElse("Unknown");
            }
            return ResponseEntity.ok((Object) EmployeeDetailResponse.builder()
                    .id(saved.getId()).firstName(saved.getFirstName()).lastName(saved.getLastName())
                    .email(saved.getEmail()).mobile(saved.getMobile()).address(saved.getAddress())
                    .username(saved.getUsername()).role(saved.getRole())
                    .branchId(saved.getBranchId()).branchName(branchName).build());
        }).orElseGet(() -> ResponseEntity.status(404).body("Employee not found"));
    }

    /**
     * PUT /api/users/employees/{id}/reset-password
     * ADMIN only — BCrypt-encodes and saves a new password.
     */
    @PutMapping("/employees/{id}/reset-password")
    public ResponseEntity<?> resetPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        String role = resolveRole(authentication);
        if (!dynamicAuthorizationService.isAllowed(role, httpRequest.getRequestURI(), httpRequest.getMethod())) {
            return ResponseEntity.status(403).body("Access denied");
        }

        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.trim().length() < 6) {
            return ResponseEntity.badRequest().body("Password must be at least 6 characters");
        }

        return employeeUserRepository.findById(id).map(emp -> {
            emp.setPassword(passwordEncoder.encode(newPassword));
            employeeUserRepository.save(emp);
            return ResponseEntity.ok((Object) "Password reset successfully");
        }).orElseGet(() -> ResponseEntity.status(404).body("Employee not found"));
    }

    // ── Utility ─────────────────────────────────────────────────────────────

    private String resolveRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return "";
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .findFirst()
                .orElse("");
    }

}