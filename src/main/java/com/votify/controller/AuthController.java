package com.votify.controller;

import com.votify.dto.UserDto;
import com.votify.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    @PostMapping("/auth/register")
    public ResponseEntity<UserDto> register(@RequestBody UserDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.register(dto.getName(), dto.getEmail(), dto.getPassword()));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<UserDto> login(@RequestBody UserDto dto) {
        return ResponseEntity.ok(userService.login(dto.getEmail(), dto.getPassword()));
    }
}
