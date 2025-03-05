package sql;

import dataaccess.DataAccessException;
import dataaccess.SqlGameDAO;
import model.game.GameData;
import org.junit.jupiter.api.BeforeEach;
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
}