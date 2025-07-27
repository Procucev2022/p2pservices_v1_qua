package com.portal.procucev.config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import com.portal.procucev.model.AuthRequest;
import com.portal.procucev.model.User;
import com.portal.procucev.service.SelfRegistrationService;
import com.portal.procucev.service.UserService;



@CrossOrigin
@RestController
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    SelfRegistrationService userService;
//
//    @PostMapping("/authenticate")
//    public ResponseEntity<Map<String, Object>> authenticate(@RequestBody AuthRequest authRequest) {
//        try {
//            authenticationManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
//            );
//        } catch (BadCredentialsException e) {
//            // Return a clear message when credentials are wrong
//            Map<String, Object> errorResponse = new HashMap<>();
//            errorResponse.put("error", "Invalid credentials");
//            errorResponse.put("error_description", "Bad credentials");
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
//        } catch (Exception e) {
//            Map<String, Object> errorResponse = new HashMap<>();
//            errorResponse.put("error", "Authentication failed: " + e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
//        }
//
//        final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUsername());
//        final String jwt = jwtUtil.generateToken(userDetails);
//
//        Map<String, Object> tokenResponse = new HashMap<>();
//        tokenResponse.put("access_token", jwt);
//        tokenResponse.put("token_type", "Bearer");
//        tokenResponse.put("expires_in", 36000); // 10 hours in seconds
//
//        return ResponseEntity.ok(tokenResponse);
//    }
//    
    @PostMapping("/authenticate")
    public ResponseEntity<Map<String, Object>> authenticate(@RequestBody AuthRequest authRequest,HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        String email=null;

        try {
            // 1. Validate if user with given email (username) and phone exists
            boolean isUserValid = userService.validateUser(authRequest.getUsername(), authRequest.getPhone());
            if (!isUserValid) {
                response.put("error", "Invalid phone number and Username");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // 2. If OTP flag is true, send OTP and exit
            if (authRequest.isOtp()) {
            	if(authRequest.getTempEmail()!=null)
            	{
            		email=authRequest.getTempEmail();
            	}
            	else {
            		email=authRequest.getUsername();
            	}
                boolean otpSent = userService.generateEmailOtp(email,request);
                if (otpSent) {
                    response.put("message", "OTP sent successfully to registered email.");
                    return ResponseEntity.ok(response);
                } else {
                    response.put("error", "Failed to send OTP.");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                }
            }

            // 3. OTP flag is false (OTP already validated), now authenticate
            String passwordToUse = authRequest.getPassword();

            // If password not provided, fetch it from DB using email and phone
            if (passwordToUse == null || passwordToUse.isBlank()) {
                passwordToUse = userService.fetchPasswordByEmailAndPhone(authRequest.getUsername(), authRequest.getPhone());
                if (passwordToUse == null) {
                    response.put("error", "Password not found for the given user.");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                }
            }

            // Authenticate with available password
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), passwordToUse)
            );

            // 4. Generate JWT Token
            final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUsername());
            final String jwt = jwtUtil.generateToken(userDetails);

            response.put("access_token", jwt);
            response.put("token_type", "Bearer");
            response.put("expires_in", 36000); // 10 hours
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            response.put("error", "Invalid credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);

        } catch (Exception e) {
            response.put("error", "Authentication failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


}