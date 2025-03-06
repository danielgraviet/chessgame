package dataaccess;

import model.auth.AuthData;

import java.sql.*;

public class SqlAuthDAO implements AuthDAO {
    public AuthData getUser(String token) throws DataAccessException {
        // create an injection statement for sql of what I want it to do.
        String sql = "SELECT username, auth_token FROM auth_tokens WHERE auth_token = ?";

        //establish the connection using a try catch method
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String authToken = rs.getString("auth_token");
                String username = rs.getString("username");
                System.out.println("DEBUG/ Found Auth Token for: " + authToken + " " + username);
                return new AuthData(username, authToken);
            }
            System.out.println("DEBUG/ No Auth Token for: " + token);
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get auth: " + e.getMessage());
        }
    }

    public void addAuthData(AuthData authData) throws DataAccessException {
        // make sure that the auth data is not in the database
        if (getUser(authData.authToken()) != null) {
            throw new DataAccessException("User already exists.");
        }

        String sql = "INSERT INTO auth_tokens (auth_token, username) VALUES (?, ?)";
        try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, authData.authToken());
            stmt.setString(2, authData.username());
            stmt.executeUpdate();
            System.out.println("DEBUG/ Added Auth Token: " + authData.authToken());
            // run code
        } catch (SQLException e) {
            throw new DataAccessException("Failed to add auth data: " + e.getMessage());
        }
    }

    public boolean removeAuthData(String token) throws DataAccessException {
        String sql = "DELETE FROM auth_tokens WHERE auth_token = ?";
        try (Connection connection = DatabaseManager.getConnection(); PreparedStatement stmt = connection.prepareStatement(sql)){
            stmt.setString(1, token);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("DEBUG/ Removed Auth Token: " + token);
                return true;
            } else {
                System.out.println("DEBUG/ No Auth Token for: " + token);
                return false;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to remove auth data: " + e.getMessage());
        }
    }

    public void clear() throws DataAccessException {
        String sql = "TRUNCATE TABLE auth_tokens";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage());
        }
    }
}
