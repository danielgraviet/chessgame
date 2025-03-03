package dataaccess;

import model.users.UserData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SqlUserDAO implements UserDAO {

    private static final Logger log = LoggerFactory.getLogger(SqlUserDAO.class);

    public void insertUser(UserData user) throws DataAccessException {
        String sql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.username());
            stmt.setString(2, user.password()); // apply hashing after.
            stmt.setString(3, user.email());
            System.out.println("Inserting user: " + user);
            stmt.executeUpdate();
            System.out.println("Insert Successful.");
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
