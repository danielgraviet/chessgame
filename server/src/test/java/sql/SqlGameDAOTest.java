package sql;

import dataaccess.DataAccessException;
import dataaccess.SqlGameDAO;
import model.game.GameData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.UUID;

public class SqlGameDAOTest {

    private SqlGameDAO gameDAO;
    private String authToken;

    @BeforeEach
    void setUp() throws DataAccessException {
        gameDAO = new SqlGameDAO();
        gameDAO.clear();
        authToken = UUID.randomUUID().toString(); // Reset the games table before each test
    }

    @Test
    @Order(1)
    @DisplayName("Create Game Successfully")
    void testCreateGameSuccess() throws DataAccessException {
        int gameId = gameDAO.createGame(authToken,"Test Game");
        assertTrue(gameId > 0, "Game ID should be positive");
        Collection<GameData> games = gameDAO.getAllGames();
        assertEquals(1, games.size(), "One game should be created");
        GameData game = games.iterator().next();
        assertEquals(gameId, game.gameID(), "Game ID should match");
        assertEquals("Test Game", game.gameName(), "Game name should match");
        assertNull(game.whiteUsername(), "White username should be null initially");
        assertNull(game.blackUsername(), "Black username should be null initially");
    }

    // get game by id test
    @Test
    @Order(2)
    @DisplayName("Get Game ID Successfully")
    void testGetGameByID() throws DataAccessException {
        // create a game
        String gameName = "Test Game";
        int gameID = gameDAO.createGame(authToken, gameName);

        // check the gameID is positive
        assertTrue(gameID > 0, "Game ID should be positive");

        // get the game by ID
        GameData retrievedGame = gameDAO.getGameByID(gameID);
        assertEquals(gameID, retrievedGame.gameID(), "Game ID should match");
        assertEquals(gameName, retrievedGame.gameName(), "Game name should match");
    }

    // update game test
    @Test
    @Order(3)
    @DisplayName("Update game successfully")
    void testUpdateGame() throws DataAccessException {
        // create a game
        String gameName = "Test Game";
        int gameID = gameDAO.createGame(authToken, gameName);

        // check the gameID is positive
        assertTrue(gameID > 0, "Game ID should be positive");

        // get original game data
        GameData originalGame = gameDAO.getGameByID(gameID);

        // update game
        String updatedGameName = "Updated Game";
        GameData updatedGameData = new GameData(
                gameID,
                "WhiteUser",
                "BlackUser",
                updatedGameName,
                originalGame.game());
        gameDAO.updateGame(updatedGameData);

        // check for success
        assertNotSame(originalGame.whiteUsername(), updatedGameData.whiteUsername(), "white username should not match");
        assertNotSame(originalGame.blackUsername(), updatedGameData.blackUsername(), "black username should not match");
        assertNotSame(originalGame.gameName(), updatedGameData.gameName(), "game name should not match");
        assertEquals(originalGame.game(), updatedGameData.game(), "Games should match");
    }

    // get all games test

    // clear test
}