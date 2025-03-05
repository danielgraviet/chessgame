package service;
import dataaccess.*;
import model.users.UserData;
import model.auth.AuthData;
import org.mindrot.jbcrypt.BCrypt;
import java.util.UUID;

public class UserService {

    UserDAO userDAO;
    AuthDAO authDAO;

    public UserService(UserDAO userDAO, AuthDAO authDAO) {
        this.userDAO = userDAO;
        this.authDAO = authDAO;

        if (userDAO == null) {
            throw new IllegalArgumentException("userDAO is null");
        }
        if (authDAO == null) {
            throw new IllegalArgumentException("authDAO is null");
        }
    }

    // this function should simply return auth data if it is valid username and password.
    public AuthData login(UserData user) throws DataAccessException {
        UserData storedUser = userDAO.getUser(user.username());
        if (storedUser != null) {
            if (BCrypt.checkpw(user.password(), storedUser.password())) {
                AuthData authData = new AuthData(user.username(), UUID.randomUUID().toString());
                authDAO.addAuthData(authData);
                return authData;
            } else {
                throw new DataAccessException("Invalid password.");
            }
        } else {
            throw new DataAccessException("User not found.");
        }
    }

    public AuthData register(UserData user) throws DataAccessException {
        // this makes sure the username is null, meaning that it does not already exist in the database.
        if (userDAO.getUser(user.username()) == null) {
            // check if it is in the database already
            AuthData authData = new AuthData(user.username(), UUID.randomUUID().toString());
            authDAO.addAuthData(authData);
            userDAO.insertUser(user);
            return authData;
        } else {
            throw new DataAccessException("User Authentication Failed.");
        }
    }

    // this is removing the auth token from the db, if it is found.
    public void logout(String token) throws DataAccessException{
        if (token != null) {
            // this is a boolean
            if (authDAO.removeAuthData(token)) {
                System.out.println("Logged out successfully.");
            } else {
                throw new DataAccessException("Invalid token.");
            }
        } else {
            throw new DataAccessException("User not found.");
        }
    }

    public void clear() throws DataAccessException {
        userDAO.clear();
        authDAO.clear();
    }
}
