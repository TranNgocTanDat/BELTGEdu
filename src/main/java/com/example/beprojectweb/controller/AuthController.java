package com.example.beprojectweb.controller;

import com.example.beprojectweb.dto.request.AuthenticationRequest;
import com.example.beprojectweb.dto.request.IntrospectRequest;
import com.example.beprojectweb.dto.request.UserCreationRequest;
import com.example.beprojectweb.dto.request.VerifyUser;
import com.example.beprojectweb.dto.response.APIResponse;
import com.example.beprojectweb.dto.response.AuthenticationResponse;
import com.example.beprojectweb.dto.response.IntrospectResponse;
import com.example.beprojectweb.entity.User;
import com.example.beprojectweb.service.AuthenticationService;
import com.example.beprojectweb.service.UserService;
import com.nimbusds.jose.JOSEException;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {
    AuthenticationService authenticationService;
    UserService userService;

    @PostMapping("/signup")
    APIResponse<User> createUser(@RequestBody @Valid UserCreationRequest request) {
        APIResponse apiResponse = new APIResponse();
        apiResponse.setResult(authenticationService.createUser(request));
        return apiResponse;
    }
    //Tạo endpoint login
    @PostMapping("/login")
    APIResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request){
        var result = authenticationService.authenticate(request);
        return APIResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/introspect")
    APIResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request) throws ParseException, JOSEException {
        var result = authenticationService.introspectResponse(request);
        return APIResponse.<IntrospectResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestBody VerifyUser request) {
        try {
            authenticationService.verifyUser(request);
            return ResponseEntity.ok("Account verified successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resendVerificationCode(@RequestParam String email) {
        try {
            authenticationService.resendVerificationCode(email);
            return ResponseEntity.ok("Verification code sent");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


}
