package com.loom.userService.dto;

import lombok.Data;

@Data
public class LoginRequestDto {
    private String email, password;
}
