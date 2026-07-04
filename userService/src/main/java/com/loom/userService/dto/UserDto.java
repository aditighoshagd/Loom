package com.loom.userService.dto;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String name, email;
}
