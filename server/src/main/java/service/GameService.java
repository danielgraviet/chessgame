package service;

import chess.ChessGame;
import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.GameDAO;
import dataaccess.UserDAO;
import model.auth.AuthData;
import model.game.GameData;

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

    // create the joinGame
    public void joinGame(String authToken, int gameID, ChessGame.TeamColor teamColor) throws DataAccessException {
        AuthData user = authDAO.getUser(authToken);
        if (user == null) {
            throw new DataAccessException("Invalid token.");
        }

        GameData game = gameDAO.getGameByID(gameID);
        if (game == null) {
            throw new DataAccessException("Game not found.");
        }

        String username = user.username();

        if (teamColor == ChessGame.TeamColor.WHITE) {
            if (!game.whiteUser().isEmpty()) {
                throw new DataAccessException("White user already exists.");
            }

            GameData updatedGame = new GameData(
                    game.gameID(),
                    username,
                    game.blackUser(),
                    game.gameName(),
                    game.game()
            );

            gameDAO.updateGame(updatedGame);
        } else if (teamColor == ChessGame.TeamColor.BLACK) {
            if (!game.blackUser().isEmpty()) {
                throw new DataAccessException("Black user already exists.");
            }

            GameData updatedGame = new GameData(
                    game.gameID(),
                    game.whiteUser(),
                    username,
                    game.gameName(),
                    game.game()
            );
            gameDAO.updateGame(updatedGame);
        } else {
            throw new DataAccessException("Invalid team color.");
        }
    }
}
