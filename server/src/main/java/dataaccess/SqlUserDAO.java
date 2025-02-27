package dataaccess;

import model.users.UserData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SqlUserDAO implements UserDAO {

    public void insertUser(UserData user) throws DataAccessException {
        String sql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.username());
            stmt.setString(2, user.password());
            stmt.setString(3, user.email());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage());
        }

    }
    public UserData getUser(String username) throws DataAccessException{
        return null;
    }

    public void clear() throws DataAccessException {

    }

}
