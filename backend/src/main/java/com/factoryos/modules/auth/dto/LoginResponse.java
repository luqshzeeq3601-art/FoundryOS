package com.factoryos.modules.auth.dto;

public class LoginResponse {
    private String accessToken;
    private long expiresInSeconds;
    private UserDto user;

    public LoginResponse() {
    }

    public LoginResponse(String accessToken, long expiresInSeconds, UserDto user) {
        this.accessToken = accessToken;
        this.expiresInSeconds = expiresInSeconds;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }
}
