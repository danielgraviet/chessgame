package service;
import dataaccess.*;
import model.users.UserData;
import model.auth.AuthData;

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
        // check login data.
        if (userDAO.authenticate(user.username(), user.password(), UserDAO.AuthMode.LOGIN)) {
            AuthData authData = new AuthData(user.username(), UUID.randomUUID().toString());
            authDAO.addAuthData(authData);
            return authData;
        } else {
            throw new DataAccessException("User Authentication Failed.");
        }
    }

    public AuthData register(UserData user) throws DataAccessException {
        if (userDAO.authenticate(user.username(), user.password(), UserDAO.AuthMode.REGISTER)) {
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
