package service;

import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.GameDAO;
import dataaccess.UserDAO;

public class GameService {
    AuthDAO authDAO;
    GameDAO gameDAO;

    public GameService(GameDAO gameDAO, AuthDAO authDAO) {
        if (gameDAO == null) {
            throw new IllegalArgumentException("userDAO is null");
        }
        if (authDAO == null) {
            throw new IllegalArgumentException("authDAO is null");
        }
        this.gameDAO = gameDAO;
        this.authDAO = authDAO;
    }

    public int createGame(String authToken, String gameName) throws DataAccessException {
        if (authDAO.getUser(authToken) == null) {
            throw new DataAccessException("Invalid token.");
        }

        return gameDAO.createGame(authToken, gameName);
    }
}
