package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.model.ApiError;
import com.kevinmazali.portfolio.model.LoginResponse;
import com.kevinmazali.portfolio.model.User;
import com.kevinmazali.portfolio.security.AnalyticsIdentityService;
import com.kevinmazali.portfolio.security.JwtService;
import com.kevinmazali.portfolio.security.SessionCookieSupport;
import com.kevinmazali.portfolio.util.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JSON login for the Vue app. Successful login sets an httpOnly session cookie (JWT);
 * admin routes authenticate via that cookie instead of client-stored Basic credentials.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Login, logout, and session introspection for the SPA")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SessionCookieSupport sessionCookieSupport;
    private final AnalyticsIdentityService analyticsIdentityService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            SessionCookieSupport sessionCookieSupport,
            AnalyticsIdentityService analyticsIdentityService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.sessionCookieSupport = sessionCookieSupport;
        this.analyticsIdentityService = analyticsIdentityService;
    }

    @Schema(description = "Username and password (same as Spring Security users)")
    public record LoginRequest(
        @Schema(example = "admin")
        String username,
        @Schema(format = "password")
        String password
    ) {}

    @Operation(summary = "Login", description = "Validates credentials, returns role, and sets an httpOnly session cookie.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authenticated",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            String username = authentication.getName();
            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            User.Role role = isAdmin ? User.Role.ADMIN : User.Role.USER;

            String jwt = jwtService.issueToken(username, role);
            sessionCookieSupport.writeSessionCookie(httpResponse, jwt);

            return ResponseEntity.ok(loginResponse(username, role));
        } catch (BadCredentialsException ex) {
            String userLabel = request.username() == null ? "<null>" : request.username();
            log.warn("Failed login attempt for username='{}' from IP={}", userLabel, ClientIpResolver.resolve(httpRequest));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError("Invalid credentials"));
        }
    }

    @Operation(summary = "Current session", description = "Returns the authenticated user from the session cookie.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authenticated",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError("Not authenticated"));
        }
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        User.Role role = isAdmin ? User.Role.ADMIN : User.Role.USER;
        return ResponseEntity.ok(loginResponse(auth.getName(), role));
    }

    private LoginResponse loginResponse(String username, User.Role role) {
        String analyticsId =
            role == User.Role.ADMIN ? analyticsIdentityService.distinctIdFor(username) : null;
        return new LoginResponse(username, role.name(), analyticsId);
    }

    @Operation(summary = "Logout", description = "Clears the session cookie.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse httpResponse) {
        sessionCookieSupport.clearSessionCookie(httpResponse);
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}
