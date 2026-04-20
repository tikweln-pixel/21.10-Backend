package com.votify.controller;

import com.votify.dto.UserDto;
import com.votify.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody UserDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.register(dto.getName(), dto.getEmail(), dto.getPassword()));
    }

    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@RequestBody UserDto dto) {
        return ResponseEntity.ok(userService.login(dto.getEmail(), dto.getPassword()));
    }
}
