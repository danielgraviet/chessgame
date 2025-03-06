package dataaccess;

import model.auth.AuthData;
import model.users.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

public class SqlUserDAOTest {

    private SqlUserDAO userDAO;
    private String authToken;

    @BeforeEach
    void setUp() throws DataAccessException {
        userDAO = new SqlUserDAO();
        userDAO.clear();
        UserData userData = new UserData("ExistingUser", UUID.randomUUID().toString(), "existing@email.com");
        userDAO.insertUser(userData); // insert one user in the db
    }

    @Test
    @Order(1)
    @DisplayName("Valid Insert User")
    void validInsertUser() throws DataAccessException {
        UserData newUserData = new UserData("NewUser", UUID.randomUUID().toString(), "new@email.com");
        userDAO.insertUser(newUserData);

        UserData retrievedUser = userDAO.getUser("NewUser");
        assertNotNull(retrievedUser);
        assertEquals(retrievedUser.username(), newUserData.username(), "usernames should match");
        assertEquals(retrievedUser.email(), newUserData.email(), "emails should match");
    }

    @Test
    @Order(2)
    @DisplayName("Invalid Insert User")
    void invalidInsertUser() throws DataAccessException {
        // try to insert a user with a taken username
        UserData newUserData = new UserData("ExistingUser", UUID.randomUUID().toString(), "new@email.com");
        try {
            userDAO.insertUser(newUserData);
            fail("Expected DataAccessException for duplicate username");
        } catch (DataAccessException e) {
            assertTrue(e.getMessage().contains("Duplicate"),
                    "Exception should indicate a duplicate username error");
        }
    }

    @Test
    @Order(3)
    @DisplayName("Valid Get User")
    void validGetUser() throws DataAccessException {
        UserData retrievedUser = userDAO.getUser("ExistingUser");
        assertNotNull(retrievedUser);
        assertEquals("ExistingUser",retrievedUser.username());
    }

    @Test
    @Order(4)
    @DisplayName("Invalid Get User")
    void invalidGetUser() throws DataAccessException {
        UserData retrievedUser = userDAO.getUser("nonExistingUser");
        assertNull(retrievedUser);
    }

}