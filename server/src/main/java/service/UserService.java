package service;
import dataaccess.*;
import model.users.UserData;
import model.auth.AuthData;

import java.util.UUID;

public class UserService {

    UserDAO userDAO;
    AuthDAO authDAO;
    MemoryUserDAO memoryUserDAO = new MemoryUserDAO();

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
        // check login data.
        if (memoryUserDAO.authenticate(user.username(), user.password())) {
            AuthData authData = new AuthData(user.username(), UUID.randomUUID().toString());
            authDAO.addAuthData(authData); // what is this?
            return authData;
        } else {
            throw new DataAccessException("User Authentication Failed.");
        }
    }

    public AuthData register(UserData user) throws DataAccessException {
        if (memoryUserDAO.authenticate(user.username(), user.password())) {
            AuthData authData = new AuthData(user.username(), UUID.randomUUID().toString());
            authDAO.addAuthData(authData);
            userDAO.insertUser(user);
            return authData;
        } else {
            throw new DataAccessException("User Authentication Failed.");
        }
    }

    public void clear() throws DataAccessException {
        userDAO.clear();
        authDAO.clear();
    }
    // make model for loginRequest and import it.
    // make model for loginResult and import it.

}
