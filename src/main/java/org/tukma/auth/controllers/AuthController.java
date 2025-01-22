package org.tukma.auth.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.tukma.auth.dtos.LoginDto;
import org.tukma.auth.dtos.SignUpDto;
import org.tukma.auth.models.UserEntity;
import org.tukma.auth.services.ModifiedUserServices;
import org.tukma.config.JwtCompilationUnit;
import org.tukma.utils.LogUtils;

import java.util.HashMap;
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

    @Autowired
    public AuthController(ModifiedUserServices userServices, AuthenticationManager authenticationManager, JwtCompilationUnit compilationUnit, ModifiedUserServices modifiedUserServices) {
        this.authenticationManager = authenticationManager;
        this.compilationUnit = compilationUnit;
        userService = userServices;
        this.modifiedUserServices = modifiedUserServices;
    }



    @PostMapping("/signup")
    public ResponseEntity<?> signUp (@Valid @RequestBody SignUpDto signUp) {
        if (userService.userExists(signUp.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("user already exists");
        }

        var returnVal = userService.createUser(signUp.getEmail(), signUp.getPassword(), signUp.getFirstName(), signUp.getLastName(), signUp.isApplicant());
        if (returnVal != null) return ResponseEntity.ok().build();
        else return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Cannot determine error in auth.");


    }



    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDto loginRequest) {
        try {
            var user = (UserEntity) modifiedUserServices.loadUserByUsername(loginRequest.getEmail());
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user,
                            loginRequest.getPassword()
                    )
            );
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



            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(Map.of("Message", "Login Successful"));


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








}
