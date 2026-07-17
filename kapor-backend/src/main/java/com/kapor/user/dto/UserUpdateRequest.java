package com.kapor.user.dto;

import com.kapor.user.model.User;
import lombok.Data;

@Data
public class UserUpdateRequest {
    private String displayName;
    private String avatarUrl;
    private String nativeLanguage;
    private User.UserSettings settings;
}
