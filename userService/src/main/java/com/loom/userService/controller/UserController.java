package com.loom.userService.controller;

import com.loom.userService.dto.LoginRequestDto;
import com.loom.userService.dto.SignupRequestDto;
import com.loom.userService.dto.UserDto;
import com.loom.userService.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<UserDto> signUp(@RequestBody SignupRequestDto signupRequestDto) {
        log.info("Processing user signup request for email: {}", signupRequestDto.getEmail());
        UserDto userDto = authService.signUp(signupRequestDto);
        return new ResponseEntity<>(userDto, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDto loginRequestDto) {
        log.info("Processing user login request for email: {}", loginRequestDto.getEmail());
        String token = authService.login(loginRequestDto);
        return ResponseEntity.ok(token);
    }
}
