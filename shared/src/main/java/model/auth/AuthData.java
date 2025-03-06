package model.auth;

public record AuthData(String username, String authToken) {
    public String username() { return username; }
    public String authToken() { return authToken; }
}
