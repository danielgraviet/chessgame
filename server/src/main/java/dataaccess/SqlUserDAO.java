package dataaccess;

import model.users.UserData;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SqlUserDAO implements UserDAO {

    private static final Logger log = LoggerFactory.getLogger(SqlUserDAO.class);

    public void insertUser(UserData user) throws DataAccessException {
        // TODO
        // hash the password to pass the bycrypt stuff.

        String sql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.username());

            String hashPassword = BCrypt.hashpw(user.password(), BCrypt.gensalt());

            stmt.setString(2, hashPassword);
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
        String sql = "TRUNCATE TABLE users";
        try (Connection connection = DatabaseManager.getConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
            System.out.println("Clear Successful.");
        } catch (SQLException e) {
            log.error("Failed to clear user table" + e.getMessage());
            throw new DataAccessException(e.getMessage());
        }
    }
}
