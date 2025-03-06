package dataaccess;

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

    @Test
    @Order(2)
    @DisplayName("Invalid Create Bad Auth")
    void testCreateInvalidGame() throws DataAccessException {
        String invalidAuthToken = null;
        String gameName = "Test Game";
        try {
            gameDAO.createGame(invalidAuthToken, gameName);
        } catch (DataAccessException e) {
            assertTrue(true, "DataAccessException was thrown");
        }
    }

    // get game by id test
    @Test
    @Order(3)
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

    @Test
    @Order(4)
    @DisplayName("Invalid Game ID")
    void testInvalidGameID() throws DataAccessException {
        // create a game
        String gameName = "Test Game";
        int gameID = gameDAO.createGame(authToken, gameName);

        // check the gameID is positive
        assertTrue(gameID > 0, "Game ID should be positive");

        // create invalid gameID
        int invalidGameID = -1;

        // get the game by invalid ID
        try {
            gameDAO.getGameByID(invalidGameID);
        } catch (DataAccessException e) {
            assertTrue(true, "DataAccessException was thrown");
        }
    }

    // update game test
    @Test
    @Order(5)
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

    // update game test
    @Test
    @Order(6)
    @DisplayName("Invalid game update")
    void testInvalidUpdateGame() throws DataAccessException {
        // create a game
        String gameName = "Test Game";
        int gameID = gameDAO.createGame(authToken, gameName);

        // check the gameID is positive
        assertTrue(gameID > 0, "Game ID should be positive");

        // get original game data
        GameData originalGame = gameDAO.getGameByID(gameID);

        // update game
        String updatedGameName = null;
        GameData updatedGameData = new GameData(
                gameID,
                "WhiteUser",
                "BlackUser",
                updatedGameName,
                originalGame.game());
        try {
            gameDAO.updateGame(updatedGameData);
        } catch (DataAccessException e) {
            assertTrue(true, "DataAccessException was thrown");
        }
        // check for success
        assertNotSame(originalGame.whiteUsername(), updatedGameData.whiteUsername(), "white username should not match");
        assertNotSame(originalGame.blackUsername(), updatedGameData.blackUsername(), "black username should not match");
        assertNotSame(originalGame.gameName(), updatedGameData.gameName(), "game name should not match");
        assertEquals(originalGame.game(), updatedGameData.game(), "Games should match");
    }

    // get all games test
    @Test
    @Order(7)
    @DisplayName("Valid get all games")
    void validGetAllGames() throws DataAccessException {
        // create game 1
        String game1 = "Game 1";
        int gameID1 = gameDAO.createGame(authToken, game1);

        // create game 1
        String game2 = "Game 2";
        int gameID2 = gameDAO.createGame(authToken, game2);

        // create game 1
        String game3 = "Game 3";
        int gameID3 = gameDAO.createGame(authToken, game3);

        // check the gameID is positive
        assertTrue(gameID1 > 0, "Game ID should be positive");
        assertTrue(gameID2 > 1, "Game ID should be positive");
        assertTrue(gameID3 > 2, "Game ID should be positive");


        Collection<GameData> games = gameDAO.getAllGames();
        assertFalse(games.isEmpty(), "Games should not be empty");

        boolean foundGame1 = false;
        boolean foundGame2 = false;
        boolean foundGame3 = false;
        for (GameData gameData : games) {
            if (gameData.gameID() == gameID1 && gameData.gameName().equals(game1)) {
                foundGame1 = true;
            } else if (gameData.gameID() == gameID2 && gameData.gameName().equals(game2)) {
                foundGame2 = true;
            } else if (gameData.gameID() == gameID3 && gameData.gameName().equals(game3)) {
                foundGame3 = true;
            }
        }
        assertTrue(foundGame1, "Game 1 should be found");
        assertTrue(foundGame2, "Game 2 should be found");
        assertTrue(foundGame3, "Game 3 should be found");
    }

    // get all games test
    @Test
    @Order(8)
    @DisplayName("Invalid get all games")
    void invalidGetAllGames() throws DataAccessException {
        Collection<GameData> games = gameDAO.getAllGames();
        assertTrue(games.isEmpty(), "Games should be empty");
        assertEquals(0, games.size(), "Game count should be zero");
    }

    // clear test
    @Test
    @Order(9)
    @DisplayName("valid clear")
    void validClear() throws DataAccessException {
        // create game 1
        String game1 = "Game 1";
        gameDAO.createGame(authToken, game1);

        // create game 1
        String game2 = "Game 2";
        gameDAO.createGame(authToken, game2);

        // create game 1
        String game3 = "Game 3";
        gameDAO.createGame(authToken, game3);

        // get all the games, and make sure they are there
        Collection<GameData> games = gameDAO.getAllGames();
        assertFalse(games.isEmpty(), "Games should not be empty");

        // clear the db
        games.clear();
        assertTrue(games.isEmpty(), "Games should be empty");
    }

    @Test
    @Order(10)
    @DisplayName("invalid clear")
    void invalidClear() throws DataAccessException {
        // create game 1
        String game1 = "Game 1";
        gameDAO.createGame(authToken, game1);

        // create game 2
        String game2 = "Game 2";
        gameDAO.createGame(authToken, game2);

        // get all the games, and make sure they are there
        Collection<GameData> games = gameDAO.getAllGames();
        assertFalse(games.isEmpty(), "Games should not be empty");

        // clear the db
        games.clear();
        assertTrue(games.isEmpty(), "Games should be empty");

        // clear the db again and check again
        games.clear();
        assertTrue(games.isEmpty(), "Games should be empty");
    }
}