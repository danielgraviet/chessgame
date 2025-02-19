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


    private static UserService userService;
    private static UserDAOUnitTest userDAOTest;
    private static AuthDAOUnitTest authDAOTest;
    private static UserData existingUser;
    private static UserData newUser;


    @BeforeAll
    public static void init() {
        // initialize the variables for setup.
        newUser = new UserData("NewUser", "NewPassword", "newUser@gmail.com");
        existingUser = new UserData("ExistingUser", "ExistingPassword", "existingUser@gmail.com");
        userDAOTest = new UserDAOUnitTest();
        authDAOTest = new AuthDAOUnitTest();
        userService = new UserService(userDAOTest, authDAOTest);
    }


    @BeforeEach
    public void setUp() {
        // I want to clear the all the storage.
        userDAOTest.clear();
        authDAOTest.clear();


        // then add 1 the existingUser.
        userDAOTest.insertUser(existingUser);
    }


    @Test
    @Order(1)
    @DisplayName("Normal User Register")
    public void normalUserRegister() throws DataAccessException {
        AuthData authData = userService.register(newUser);


        assertNotNull(authData, "AuthData should not be null");
        assertNotNull(authData.authToken(), "Auth token should not be null");
        assertEquals(newUser.username(), authData.username(), "Usernames should match");
    }


    @Test
    @Order(2)
    @DisplayName("Invalid User Register")
    public void InvalidUserRegister() throws DataAccessException {
        // existing user is already in db, now trying to register again.
        assertThrows(DataAccessException.class, () -> {
            userService.register(existingUser);
        }, "Should throw DataAccessException for duplicate user");
    }


    @Test
    @Order(3)
    @DisplayName("Normal User Login")
    public void normalUserLogin() throws DataAccessException {
        // create the account.
        AuthData authData = userService.login(existingUser);


        assertNotNull(authData, "AuthData should not be null");
        assertNotNull(authData.authToken(), "Auth token should not be null");
        assertEquals(existingUser.username(), authData.username(), "Usernames should match");
    }


    @Test
    @Order(4)
    @DisplayName("Invalid User Login")
    public void invalidUserLogin() throws DataAccessException {
        // login to an account that does not exist.
        assertThrows(DataAccessException.class, () -> {
            userService.login(newUser);
        }, "Should throw DataAccessException for invalid user");
    }


    @Test
    @Order(5)
    @DisplayName("Normal User Logout")
    public void normalUserLogout() throws DataAccessException {
        // get auth token, and remove it from db
        AuthData authData = userService.login(existingUser);
        String authToken = authData.authToken();
        userService.logout(authToken);


        assertNull(authDAOTest.getUser(authToken),
                "Auth token should no longer exist in AuthDAO after logout");
    }


    @Test
    @Order(6)
    @DisplayName("Invalid User Logout")
    public void invalidUserLogout() throws DataAccessException {
        // register new user.
        userService.register(newUser);
        AuthData authData = userService.login(newUser);
        String authToken = authData.authToken();

        // logout and remove token
        userService.logout(authToken);

        // second logout.
        userService.logout(authToken);

        // Verify the token is still not present in AuthDAO
        assertNull(authDAOTest.getUser(authToken),
                "Auth token should remain absent after repeated logout attempts");
    }

    @Test
    @Order(7)
    @DisplayName("Valid Clear")
    public void validClear() throws DataAccessException {
        // add users
        userService.register(newUser);
        AuthData authData = userService.login(newUser);

        // clear db
        userService.clear();

        // now try adn access empty db
        assertNull(userDAOTest.getUser(newUser.username()), "UserDAO should no longer contain data.");
        assertNull(authDAOTest.getUser(authData.authToken()), "Auth token should no longer exist");
    }

    @Test
    @Order(8)
    @DisplayName("invalid Clear - empty database")
    public void invalidClear() throws DataAccessException {
        // clear the db
        userService.clear();

        // clear again
        userService.clear();

        // make sure no data exists (redundant but confirms state)
        assertNull(userDAOTest.getUser(newUser.username()), "UserDAO should be empty.");
        assertNull(authDAOTest.getUser("anyToken"), "AuthDAO should be empty.");
    }
















    // setup stuff for local storage.
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
        public boolean removeAuthData(String token) throws DataAccessException {
            return authDataMap.remove(token) != null;
        }


        @Override
        public void clear() {
            authDataMap.clear();
        }
    }
}

