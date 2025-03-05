package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.game.GameData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class SqlGameDAO implements GameDAO {

    private static final Gson GSON = new Gson();

    public int createGame(String authToken, String gameName) throws DataAccessException {
        ChessGame chessGame = new ChessGame();
        String sql = "INSERT INTO games (game_name, white_username, black_username, game_data) VALUES (?, NULL, NULL, ?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, gameName);
            stmt.setString(2, GSON.toJson(chessGame));
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new DataAccessException("Game creation failed");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage());
        }
    }

    public GameData getGameByID(int gameID) throws DataAccessException {
        String sql = "SELECT game_id, game_name, white_username, black_username, game_data FROM games WHERE game_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, gameID);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String gameDataJson = rs.getString("game_data");
                    ChessGame chessGame = gameDataJson != null ? GSON.fromJson(gameDataJson, ChessGame.class) : null;
                    return new GameData(
                            rs.getInt("game_id"),
                            rs.getString("white_username"),
                            rs.getString("black_username"),
                            rs.getString("game_name"),
                            chessGame
                    );
                }
                return null; // Game not found
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error retrieving game: " + e.getMessage());
        }
    }

    public void updateGame(GameData gameData) throws DataAccessException {

        String whiteUsername = resolveUsername(gameData.whiteUsername());
        String blackUsername = resolveUsername(gameData.blackUsername());

        String sql = "UPDATE games SET white_username = ?, black_username = ?, game_data = ? WHERE game_id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, whiteUsername);
            stmt.setString(2, blackUsername);
            stmt.setString(3, gameData.game() != null ? GSON.toJson(gameData.game()) : null);
            stmt.setInt(4, gameData.gameID());
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new DataAccessException("Game update failed. Game ID: " + gameData.gameID() + " not found.");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update game: " + e.getMessage());
        }
    }

    public Collection<GameData> getAllGames() throws DataAccessException {
        Collection<GameData> games = new ArrayList<>(); // Always initialize, never null
        String sql = "SELECT game_id, game_name, white_username, black_username, game_data FROM games";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String gameDataJson = rs.getString("game_data");
                ChessGame chessGame = gameDataJson != null ? GSON.fromJson(gameDataJson, ChessGame.class) : null;
                games.add(new GameData(
                        rs.getInt("game_id"),
                        rs.getString("white_username"),
                        rs.getString("black_username"),
                        rs.getString("game_name"),
                        chessGame
                ));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error retrieving games: " + e.getMessage());
        }
        return games;
    }

    public void clear() throws DataAccessException{
        String sql = "TRUNCATE TABLE games";
        try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage());
        }
    }

    private String resolveUsername(String input) throws DataAccessException {
        // If input is null or not a UUID, assume it's a username
        if (input == null) {
            return null;
        }
        try {
            UUID.fromString(input); // Check if it's a UUID
        } catch (IllegalArgumentException e) {
            return input; // Not a UUID, treat as username
        }

        // It's a UUID, look up the username in auth_tokens
        String sql = "SELECT username FROM auth_tokens WHERE auth_token = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, input);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
                return null; // Auth token not found
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to resolve auth token: " + e.getMessage());
        }
    }
}
