package com.alert.platform.controller;

import com.alert.platform.dto.ApiResponse;
import com.alert.platform.entity.User;
import com.alert.platform.repository.UserRepository;
import com.alert.platform.security.JwtUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证Controller
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 登录请求DTO
     */
    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    /**
     * 登录响应DTO
     */
    @Data
    public static class LoginResponse {
        private String token;
        private String username;
        private String role;
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            String token = jwtUtil.generateToken(request.getUsername());

            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setUsername(user.getUsername());
            response.setRole(user.getRole());

            log.info("用户登录成功: {}", request.getUsername());
            return ApiResponse.success("登录成功", response);

        } catch (AuthenticationException e) {
            log.error("登录失败: {}", e.getMessage());
            return ApiResponse.error("用户名或密码错误");
        }
    }

    /**
     * 初始化管理员账户 (仅用于首次部署)
     */
    @PostMapping("/init-admin")
    public ApiResponse<String> initAdmin(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (userRepository.existsByUsername(username)) {
            return ApiResponse.error("用户已存在");
        }

        User admin = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role("ADMIN")
                .enabled(true)
                .build();

        userRepository.save(admin);

        log.info("管理员账户创建成功: {}", username);
        return ApiResponse.success("管理员账户创建成功");
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "auth");
        return ApiResponse.success(status);
    }
}
