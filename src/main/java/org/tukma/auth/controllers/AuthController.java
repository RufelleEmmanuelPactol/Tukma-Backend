package org.tukma.auth.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.tukma.auth.dtos.LoginDto;
import org.tukma.auth.dtos.SignUpDto;
import org.tukma.auth.models.UserEntity;
import org.tukma.auth.services.ModifiedUserServices;
import org.tukma.config.JwtCompilationUnit;
import org.tukma.utils.LogUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Controller
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtCompilationUnit compilationUnit;
    private final ModifiedUserServices userService;
    private final ModifiedUserServices modifiedUserServices;
    private final RedisTemplate<String, Object> ticketGenerator;

    @Autowired
    public AuthController(ModifiedUserServices userServices, AuthenticationManager authenticationManager,
            JwtCompilationUnit compilationUnit, ModifiedUserServices modifiedUserServices,
            RedisTemplate<String, Object> ticketGenerator) {
        this.authenticationManager = authenticationManager;
        this.compilationUnit = compilationUnit;
        userService = userServices;
        this.modifiedUserServices = modifiedUserServices;
        this.ticketGenerator = ticketGenerator;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@Valid @RequestBody Map<String, Object> requestBody) {
        // Extract data from request body to ensure proper boolean handling
        String email = (String) requestBody.get("email");
        String password = (String) requestBody.get("password");
        String firstName = (String) requestBody.get("firstName");
        String lastName = (String) requestBody.get("lastName");
        Boolean isApplicant = (Boolean) requestBody.get("isApplicant");
        String companyName = (String) requestBody.get("companyName");

        // Default to true if not provided (assuming most users will be applicants)
        boolean isUserApplicant = isApplicant != null ? isApplicant : true;

        if (userService.userExists(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("user already exists");
        }

        var returnVal = userService.createUser(email, password, firstName, lastName, isUserApplicant, companyName);

        // If hasJob is explicitly provided in the request, update it
        if (requestBody.containsKey("hasJob") && returnVal != null) {
            Boolean hasJob = (Boolean) requestBody.get("hasJob");
            returnVal.setHasJob(hasJob);
            userService.saveUser(returnVal);
        }

        if (returnVal != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Cannot determine error in auth.");
        }
    }

    // generate random 12-character string
    private String generateTicket() {
        String ticket = "";
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < 12; i++) {
            int index = (int) (characters.length() * Math.random());
            ticket += characters.charAt(index);
        }
        return ticket;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDto loginRequest) {
        try {
            var user = (UserEntity) modifiedUserServices.loadUserByUsername(loginRequest.getEmail());
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user,
                            loginRequest.getPassword()));
            JwtCompilationUnit.TransientJwt transientJwt = JwtCompilationUnit.startTransientState();
            transientJwt.addUsername(loginRequest.getEmail());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = transientJwt.toString();
            ResponseCookie cookie = ResponseCookie.from("jwt", token)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(86400)
                    .sameSite("Strict")
                    .build();
            String ticket = generateTicket();
            ticketGenerator.opsForValue().set(ticket, authentication);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(Map.of("Message", "Login Successful", "ticket", ticket));

        } catch (Exception ex) {
            Logger.getGlobal().info("Some exception happened during auth: " + LogUtils.getStackTraceAsString(ex));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", ex.getLocalizedMessage()));
        }

    }

    @GetMapping("/user-status")
    public ResponseEntity<?> userStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "User not authenticated"));
        }
        var currentUser = auth.getPrincipal();
        return ResponseEntity.ok(Map.of("userDetails", currentUser));
    }

    /**
     * Update the hasJob field for the currently authenticated user
     * 
     * @param requestBody Map containing the hasJob boolean value
     * @return Updated user details
     */
    @PostMapping("/update-job-status")
    public ResponseEntity<?> updateJobStatus(@RequestBody Map<String, Object> requestBody) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "User not authenticated"));
        }

        UserEntity currentUser = (UserEntity) auth.getPrincipal();

        // Update the hasJob field if provided
        if (requestBody.containsKey("hasJob")) {
            Boolean hasJob = (Boolean) requestBody.get("hasJob");
            currentUser.setHasJob(hasJob);
            userService.saveUser(currentUser);
        }

        return ResponseEntity.ok(Map.of("userDetails", currentUser));
    }

    /**
     * Get users by their hasJob status
     * 
     * @param hasJob The hasJob status to filter by (true, false, or null)
     * @return List of users matching the hasJob status
     */
    @GetMapping("/users-by-job-status")
    public ResponseEntity<?> getUsersByJobStatus(@RequestParam(required = false) Boolean hasJob) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "User not authenticated"));
        }

        // For security, check if the user is an admin or recruiter
        UserEntity currentUser = (UserEntity) auth.getPrincipal();
        if (!currentUser.isRecruiter()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Only recruiters can view users by job status"));
        }

        List<UserEntity> users = userService.getUsersByJobStatus(hasJob);

        return ResponseEntity.ok(Map.of(
                "users", users,
                "count", users.size()));
    }

    /**
     * Batch update hasJob status for multiple users (admin function)
     * 
     * @param requestBody Map containing an array of user updates with userId and
     *                    hasJob values
     * @return Status message with count of updated users
     */
    @PostMapping("/batch-update-job-status")
    public ResponseEntity<?> batchUpdateJobStatus(@RequestBody Map<String, Object> requestBody) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "User not authenticated"));
        }

        // For security, check if the user is an admin or recruiter
        UserEntity currentUser = (UserEntity) auth.getPrincipal();
        if (!currentUser.isRecruiter()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Only recruiters can perform batch updates"));
        }

        // Extract the updates array
        List<Map<String, Object>> updates = (List<Map<String, Object>>) requestBody.get("updates");
        if (updates == null || updates.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "No updates provided"));
        }

        int updatedCount = 0;
        for (Map<String, Object> update : updates) {
            if (update.containsKey("userId") && update.containsKey("hasJob")) {
                Long userId = ((Number) update.get("userId")).longValue();
                Boolean hasJob = (Boolean) update.get("hasJob");

                boolean success = userService.updateUserJobStatus(userId, hasJob);
                if (success) {
                    updatedCount++;
                }
            }
        }

        return ResponseEntity.ok(Map.of(
                "message", "Batch update completed",
                "updatedUsers", updatedCount));
    }

}
