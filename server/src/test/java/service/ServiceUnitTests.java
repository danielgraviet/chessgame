package service;
import chess.ChessGame;
import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.UserDAO;
import dataaccess.GameDAO;
import model.auth.AuthData;
import model.game.GameData;
import org.junit.jupiter.api.*;
import model.users.UserData;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceUnitTests {
    private static UserService userService;
    private static GameService gameService;
    private static GameDAOUnitTest gameDAOTest;
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
        gameDAOTest = new GameDAOUnitTest();
        gameService = new GameService(gameDAOTest,authDAOTest);
        userService = new UserService(userDAOTest, authDAOTest);
    }


    @BeforeEach
    public void setUp() {
        // I want to clear the all the storage.
        userDAOTest.clear();
        authDAOTest.clear();
        gameDAOTest.clear();

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
    public void invalidUserRegister() {
        // existing user is already in db, now trying to register again.
        assertThrows(DataAccessException.class, () -> userService.register(existingUser), "Should throw DataAccessException for duplicate user");
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
    public void invalidUserLogin() {
        // login to an account that does not exist.
        assertThrows(DataAccessException.class, () -> userService.login(newUser), "Should throw DataAccessException for invalid user");
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


    @Test
    @Order(9)
    @DisplayName("Valid create game")
    public void validCreateGame() throws DataAccessException {
        // first get a token to create a game.
        AuthData authData = userService.login(existingUser);
        String authToken = authData.authToken();

        int gameID = gameService.createGame(authToken, "TestGame");
        assertTrue(gameID > 0, "Game ID should be greater than 0");
    }

    @Test
    @Order(10)
    @DisplayName("Invalid create game - Duplicate Game")
    public void invalidCreateGame() throws DataAccessException {
        // first get a token to create a game.
        AuthData authData = userService.login(existingUser);
        String authToken = authData.authToken();

        int gameID = gameService.createGame(authToken, "TestGame");
        assertTrue(gameID > 0, "Game ID should be greater than 0");

        assertThrows(DataAccessException.class,
                () -> gameService.createGame(authToken, "TestGame"),
                "Should throw DataAccessException for duplicate game name");
    }

    @Test
    @Order(11)
    @DisplayName("Valid join game")
    public void validJoinGame() throws DataAccessException {
        // first get a token to create a game.
        AuthData authData = userService.login(existingUser);
        String authToken = authData.authToken();

        int gameID = gameService.createGame(authToken, "TestGame");
        assertTrue(gameID > 0, "Game ID should be greater than 0");

        // join that game with another user
        userService.register(newUser);
        AuthData newAuthData = userService.login(newUser);
        String newAuthToken = newAuthData.authToken();

        // auth token, int gameID, teamColor
        gameService.joinGame(newAuthToken, gameID, ChessGame.TeamColor.BLACK);
        GameData game = gameDAOTest.getGameByID(gameID);
        assertNotNull(game, "game should exist.");
        assertEquals(newUser.username(), game.blackUsername(), "black username should be newUser");
        assertNull(game.whiteUsername(), "white username should still be null");
    }

    @Test
    @Order(12)
    @DisplayName("Invalid join game - Duplicate team color")
    public void invalidJoinGame() throws DataAccessException {
        // first get a token to create a game.
        AuthData authData = userService.login(existingUser);
        String authToken = authData.authToken();

        int gameID = gameService.createGame(authToken, "TestGame");
        assertTrue(gameID > 0, "Game ID should be greater than 0");

        // join that game with another user
        userService.register(newUser);
        AuthData newAuthData = userService.login(newUser);
        String newAuthToken = newAuthData.authToken();

        // auth token, int gameID, teamColor
        gameService.joinGame(newAuthToken, gameID, ChessGame.TeamColor.BLACK);

        // try to add another black player
        assertThrows(DataAccessException.class,
                () -> gameService.joinGame(newAuthToken, gameID, ChessGame.TeamColor.BLACK),
                "Should throw DataAccessException for two black users.");
    }

    @Test
    @Order(13)
    @DisplayName("Valid get all games")
    public void validGetAllGames() throws DataAccessException {
        // first get a token to create a game.
        AuthData authData = userService.login(existingUser);
        String authToken = authData.authToken();

        int gameID = gameService.createGame(authToken, "TestGame");
        int gameID2 = gameService.createGame(authToken, "TestGame2");
        int gameID3 = gameService.createGame(authToken, "TestGame3");
        assertTrue(gameID > 0, "Game ID 1 should be greater than 0");
        assertTrue(gameID2 > 0, "Game ID 2 should be greater than 0");
        assertTrue(gameID3 > 0, "Game ID 3 should be greater than 0");

        Collection<GameData> games = gameService.getAllGames(authToken);

        assertNotNull(games, "games should not be null");
        assertFalse(games.isEmpty(), "games should not be empty");

        Set<String> gameNames = new HashSet<>();
        Set<Integer> gameIDs = new HashSet<>();
        for (GameData game : games) {
            gameNames.add(game.gameName());
            gameIDs.add(game.gameID());
        }
        assertTrue(gameNames.contains("TestGame"), "Should contain TestGame");
        assertTrue(gameNames.contains("TestGame2"), "Should contain TestGame2");
        assertTrue(gameNames.contains("TestGame3"), "Should contain TestGame3");
        assertTrue(gameIDs.contains(gameID), "Should contain game ID 1");
        assertTrue(gameIDs.contains(gameID2), "Should contain game ID 2");
        assertTrue(gameIDs.contains(gameID3), "Should contain game ID 3");
    }

    @Test
    @Order(14)
    @DisplayName("invalid get all games - bad token")
    public void invalidGetAllGames() {
       assertThrows(DataAccessException.class, () -> gameService.getAllGames(null), "Should throw DataAccessException for bad token");
    }

    @Test
    @Order(15)
    @DisplayName("valid clear games")
    public void validClearGames() throws DataAccessException {
        // first get a token to create a game.
        AuthData authData = userService.login(existingUser);
        String authToken = authData.authToken();

        int gameID = gameService.createGame(authToken, "TestGame");
        int gameID2 = gameService.createGame(authToken, "TestGame2");
        int gameID3 = gameService.createGame(authToken, "TestGame3");
        assertTrue(gameID > 0, "Game ID 1 should be greater than 0");
        assertTrue(gameID2 > 0, "Game ID 2 should be greater than 0");
        assertTrue(gameID3 > 0, "Game ID 3 should be greater than 0");

        Collection<GameData> games = gameService.getAllGames(authToken);

        // make sure games are being created.
        assertNotNull(games, "games should not be null");
        assertFalse(games.isEmpty(), "games should not be empty");

        // clear games
        gameService.clear();

        assertTrue(games.isEmpty(), "games should be empty");
    }

    @Test
    @Order(16)
    @DisplayName("invalid clear games - already empty")
    public void invalidClearGames() throws DataAccessException {
        // first get a token to create a game.
        AuthData authData = userService.login(existingUser);
        String authToken = authData.authToken();

        Collection<GameData> games = gameService.getAllGames(authToken);
        gameService.clear();

        assertTrue(games.isEmpty(), "games should be empty");

        // clear games again & verify still empty
        gameService.clear();
        assertTrue(games.isEmpty(), "games should remain empty");
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


        @Override
        public void clear() {
            UserStorageUnitTest.clear();
        }


    }


    private static class AuthDAOUnitTest implements AuthDAO {
        private final Map<String, AuthData> authDataMap = new HashMap<>();

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

    private static class GameDAOUnitTest implements GameDAO {

        private final HashSet<GameData> gameStorageTest = new HashSet<>();
        private static final AtomicInteger NextGameID = new AtomicInteger(1);

        @Override
        public int createGame(String authToken, String gameName) throws DataAccessException {

            if (!gameStorageTest.isEmpty()) {
                for (GameData gameData : gameStorageTest) {
                    if (gameData.gameName().equals(gameName)) {
                        throw new DataAccessException("Game name already exists");
                    }
                }
            }

            int gameID = NextGameID.getAndIncrement();

            // enter names for white and black user
            String blackUsername = null;
            String whiteUsername = null;

            // pass in and set the game name,

            // create a new chessGame Object.
            ChessGame chessGame = new ChessGame();
            GameData game = new GameData(gameID, whiteUsername, blackUsername, gameName, chessGame);
            gameStorageTest.add(game);
            return gameID;
        }

        @Override
        public GameData getGameByID(int gameID) throws DataAccessException {
            for (GameData game: gameStorageTest) {
                if (game.gameID() == gameID) {
                    return game;
                }
            }
            throw new DataAccessException("Game not found");
        }

        @Override
        public void updateGame(GameData gameData) throws DataAccessException {
            boolean removed = gameStorageTest.removeIf(game -> game.gameID() == gameData.gameID());
            if (removed) {
                gameStorageTest.add(gameData);
            } else {
                throw new DataAccessException("Game not found for update.");
            }
        }

        @Override
        public Collection<GameData> getAllGames() {
            return gameStorageTest;
        }

        @Override
        public void clear() {
            gameStorageTest.clear();
            NextGameID.set(1);
        }
    }
}

