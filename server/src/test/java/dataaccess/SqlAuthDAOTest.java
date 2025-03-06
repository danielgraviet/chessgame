package dataaccess;

import model.auth.AuthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    @Order(2)
    @DisplayName("Invalid getUser ")
    void invalidGetUser() throws DataAccessException {
        String invalidToken = UUID.randomUUID().toString();
        AuthData userData = authDAO.getUser(invalidToken);
        assertNull(userData);
    }

    @Test
    @Order(3)
    @DisplayName("Valid addAuthData")
    void validAddAuthData() throws DataAccessException {
        // create new token and username
        String username = "newUsername";
        String newAuthToken = UUID.randomUUID().toString();

        // add it to db
        authDAO.addAuthData(new AuthData(username, newAuthToken));

        // fetch from db
        AuthData retrievedAuthData = authDAO.getUser(newAuthToken);
        assertNotNull(retrievedAuthData, "Auth data should not be null");
        assertEquals(username, retrievedAuthData.username(), "Auth username should match");
        assertEquals(newAuthToken, retrievedAuthData.authToken(), "Auth token should match");
    }

    @Test
    @Order(4)
    @DisplayName("Invalid addAuthData")
    void invalidAddAuthData() throws DataAccessException {
        // create an invalid token and username
        String username = "newUsername";
        String newAuthToken = null;

        // try to add it to db
        try {
            authDAO.addAuthData(new AuthData(username, newAuthToken));
        } catch (DataAccessException e) {
            AuthData retrievedAuthData = authDAO.getUser(newAuthToken);
            assertNull(retrievedAuthData, "Auth data should be null");
        }
    }

    @Test
    @Order(5)
    @DisplayName("Valid removeAuthData")
    void validRemoveAuthData() throws DataAccessException {
        authDAO.removeAuthData(authToken);
        AuthData retrievedAuthData = authDAO.getUser(authToken);
        assertNull(retrievedAuthData, "Auth data should be null because it has been removed");
    }

    @Test
    @Order(6)
    @DisplayName("Invalid removeAuthData")
    void invalidRemoveAuthData() throws DataAccessException {
        // remove an authToken that is not in the db
        String invalidAuthToken = UUID.randomUUID().toString();

        // try to remove it
        try {
            authDAO.removeAuthData(invalidAuthToken);
        } catch (DataAccessException e) {
            AuthData retrievedAuthData = authDAO.getUser(invalidAuthToken);
            assertNull(retrievedAuthData, "Auth data should be null");
        }
    }

    @Test
    @Order(7)
    @DisplayName("Valid Clear")
    void validClear() throws DataAccessException {
        // add auth token
        String username = "newUsername";
        String newAuthToken = UUID.randomUUID().toString();

        // confirm in db
        authDAO.addAuthData(new AuthData(username, newAuthToken));
        assertEquals(username, authDAO.getUser(newAuthToken).username(), "Auth username should match in the db");

        // clear db
        authDAO.clear();
        AuthData retrievedAuthData = authDAO.getUser(username);
        assertNull(retrievedAuthData, "Auth data should longer exist after clear");
    }

    @Test
    @Order(8)
    @DisplayName("Invalid clear")
    void invalidClear() throws DataAccessException {
        // add auth token
        String username = "newUsername";
        String newAuthToken = UUID.randomUUID().toString();

        // confirm in db
        authDAO.addAuthData(new AuthData(username, newAuthToken));
        assertEquals(username, authDAO.getUser(newAuthToken).username(), "Auth username should match in the db");

        // clear db
        authDAO.clear();
        AuthData retrievedAuthData = authDAO.getUser(username);
        assertNull(retrievedAuthData, "Auth data should longer exist after clear");

        // clear empty db
        authDAO.clear();
        AuthData retrievedAuthData2 = authDAO.getUser(newAuthToken);
        assertNull(retrievedAuthData2, "Auth data should still be null after 2nd clear");
    }


}