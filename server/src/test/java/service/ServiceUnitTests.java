package service;
import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.UserDAO;
import model.auth.AuthData;
import org.junit.jupiter.api.*;
import model.users.UserData;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceUnitTests {
    /* TODO
    *  figure out what variables I need to do testing
    * initialize them
    * figure out after all, and before all.
    *  */

    private UserService userService;

    @BeforeEach
    public void setUp() {
        UserDAOUnitTest userDAOTest = new UserDAOUnitTest();
        AuthDAOUnitTest authDAOTest = new AuthDAOUnitTest();
        userService = new UserService(userDAOTest, authDAOTest);
    }

    @Test
    @Order(1)
    @DisplayName("Normal User Register")
    public void normalUserRegister() throws DataAccessException {
        UserData newUser = new UserData("TestUser", "TestPassword", "TestUser@gmail.com");
        AuthData authData = userService.register(newUser);

        assertNotNull(authData, "AuthData should not be null");
        assertNotNull(authData.authToken(), "Auth token should not be null");
        assertEquals(newUser.username(), authData.username(), "Usernames should match");
    }


    private static class UserDAOUnitTest implements UserDAO {
        private final HashSet<UserData> UserStorageUnitTest = new HashSet<>();

        @Override
        public void insertUser (UserData user) {
            UserStorageUnitTest.add(user);
        }

        @Override
        public UserData getUser(String username) {
            return UserStorageUnitTest.stream().filter(user -> user.username().equals(username)).findFirst().orElse(null);
        }

        // these aren't being used, so eventually remove them.
        @Override
        public void updateUser (UserData user) {
            UserStorageUnitTest.remove(user);
        }

        // these aren't being used, so eventually remove them.
        @Override
        public void deleteUser (UserData user) {
            UserStorageUnitTest.remove(user);
        }

        @Override
        public void clear() {
            UserStorageUnitTest.clear();
        }

    }

    private static class AuthDAOUnitTest implements AuthDAO {
        private final Map<String, AuthData> authDataMap = new HashMap<>();

        @Override
        public AuthData getAuthData(String username, String password) throws DataAccessException{
            if (!authDataMap.containsKey(username)) {
                throw new DataAccessException("User not found");
            }
            return authDataMap.get(username);
        }

        @Override
        public AuthData getUser(String token) {
            return authDataMap.get(token);
        }

        @Override
        public void addAuthData(AuthData authData) {
            authDataMap.put(authData.authToken(), authData);
        }

        @Override
        public boolean removeAuthData(String token) {
            return authDataMap.remove(token) != null;
        }

        @Override
        public void clear() {
            authDataMap.clear();
        }
    }
}
