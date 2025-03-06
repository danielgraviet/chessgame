package dataaccess;

import model.auth.AuthData;
import model.game.GameData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.UUID;

public class SqlAuthDAOTest {

    private SqlAuthDAO authDAO;
    private String authToken;

    @BeforeEach
    void setUp() throws DataAccessException {
        authDAO = new SqlAuthDAO();
        authDAO.clear();
        authToken = UUID.randomUUID().toString(); // Reset the games table before each test
        authDAO.addAuthData(new AuthData("ExistingUser", authToken)); // add 1 user to db
    }

    @Test
    @Order(1)
    @DisplayName("Get User Successfully")
    void getUserSuccessfully() throws DataAccessException {
        AuthData userData = authDAO.getUser(authToken);
        assertNotNull(userData);
        assertEquals(authToken,  userData.authToken(), "Auth token does not match");
        assertEquals("ExistingUser", userData.username(), "Auth username should match");
    }


}