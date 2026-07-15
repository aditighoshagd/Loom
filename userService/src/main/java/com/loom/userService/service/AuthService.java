package com.loom.userService.service;

import com.loom.userService.dto.LoginRequestDto;
import com.loom.userService.dto.SignupRequestDto;
import com.loom.userService.dto.UserDto;
import com.loom.userService.entity.User;
import com.loom.userService.event.UserCreatedEvent;
import com.loom.userService.exception.BadRequestException;
import com.loom.userService.exception.ResourceNotFoundException;
import com.loom.userService.repository.UserRepository;
import com.loom.userService.utils.BCrypt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final JwtService jwtService;
    private final KafkaTemplate<Long, UserCreatedEvent> userCreatedEventKafkaTemplate;

    public UserDto signUp(SignupRequestDto signupRequestDto) {
        log.info("Signup a user with email: {}", signupRequestDto.getEmail());

        boolean exists = userRepository.existsByEmail(signupRequestDto.getEmail());
        if(exists) {
            throw new BadRequestException("User already exists");
        }

        User user = modelMapper.map(signupRequestDto, User.class);
        user.setPassword(BCrypt.hash(signupRequestDto.getPassword()));

        user = userRepository.save(user);

        UserCreatedEvent userCreatedEvent = UserCreatedEvent.builder()
                .userId(user.getId())
                .name(user.getName())
                .build();

        userCreatedEventKafkaTemplate.send("user_created_topic", userCreatedEvent);

        return modelMapper.map(user, UserDto.class);
    }

    public String login(LoginRequestDto loginRequestDto) {
        log.info("Login request for user with email: {}", loginRequestDto.getEmail());

        User user = userRepository.findByEmail(loginRequestDto.getEmail()).orElseThrow(() -> new BadRequestException(
                "Incorrect email or password"));

        boolean isPasswordMatch = BCrypt.match(loginRequestDto.getPassword(), user.getPassword());

        if(!isPasswordMatch) {
            throw new BadRequestException("Incorrect email or password");
        }

        return jwtService.generateAccessToken(user);
    }

    public UserDto updateProfilePicture(Long userId, String profilePictureUrl) {
        log.info("Updating profile picture for user with ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        user.setProfilePictureUrl(profilePictureUrl);
        user = userRepository.save(user);
        return modelMapper.map(user, UserDto.class);
    }

    public UserDto getUserById(Long userId) {
        log.info("Fetching profile for user with ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return modelMapper.map(user, UserDto.class);
    }
}
