package service;

import chess.ChessGame;
import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.GameDAO;
import model.auth.AuthData;
import model.game.GameData;

import java.util.Collection;

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
            if (game.whiteUsername() != null) {
                throw new DataAccessException("White user already exists.");
            }

            GameData updatedGame = new GameData(
                    game.gameID(),
                    username,
                    game.blackUsername(),
                    game.gameName(),
                    game.game()
            );

            gameDAO.updateGame(updatedGame);
        } else if (teamColor == ChessGame.TeamColor.BLACK) {
            if (game.blackUsername() != null) {
                throw new DataAccessException("Black user already exists.");
            }

            GameData updatedGame = new GameData(
                    game.gameID(),
                    game.whiteUsername(),
                    username,
                    game.gameName(),
                    game.game()
            );
            gameDAO.updateGame(updatedGame);
        } else {
            throw new DataAccessException("Invalid team color.");
        }
    }

    public Collection<GameData> getAllGames(String authToken) throws DataAccessException {
        if (authDAO.getUser(authToken) == null) {
            throw new DataAccessException("Invalid token.");
        } else {
            return gameDAO.getAllGames(); // fix me
        }
    }

    public void clear() throws DataAccessException {
        gameDAO.clear();
    }
}
