package com.kapor.admin.service;

import com.kapor.admin.dto.CreateUserRequest;
import com.kapor.user.model.User;
import com.kapor.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminServiceTest {

    @Test
    void createUserAssignsCanonicalEmailAsProviderId() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail(" Test.User@Example.COM ");
        request.setPassword("password123");
        request.setName("Test User");
        request.setRole("USER");

        AtomicReference<User> savedUser = new AtomicReference<>();
        UserRepository userRepository = (UserRepository) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "existsByEmail" -> false;
                    case "save" -> {
                        savedUser.set((User) args[0]);
                        yield args[0];
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        PasswordEncoder passwordEncoder = new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return "encoded-password";
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return false;
            }
        };
        AdminService adminService = new AdminService(userRepository, null, null, passwordEncoder);

        adminService.createUser(request);

        User createdUser = savedUser.get();
        assertEquals("email", createdUser.getProvider());
        assertEquals("test.user@example.com", createdUser.getProviderId());
        assertEquals("test.user@example.com", createdUser.getEmail());
    }
}
