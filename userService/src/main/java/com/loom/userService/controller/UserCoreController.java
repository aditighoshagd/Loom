package com.loom.userService.controller;

import com.loom.userService.dto.UserDto;
import com.loom.userService.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/core")
@RequiredArgsConstructor
public class UserCoreController {

    private final AuthService authService;

    @PutMapping("/profile-picture")
    public ResponseEntity<UserDto> updateProfilePicture(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody String profilePictureUrl) {
        UserDto userDto = authService.updateProfilePicture(userId, profilePictureUrl);
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long userId) {
        UserDto userDto = authService.getUserById(userId);
        return ResponseEntity.ok(userDto);
    }
}
