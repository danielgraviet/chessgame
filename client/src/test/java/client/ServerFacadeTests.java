package client;

import model.auth.AuthData;
import model.game.GameData;
import org.junit.jupiter.api.*;
import server.Server;

import java.util.Collection;

public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade facade;
    private static int port;

    @BeforeAll
    public static void init() {
        server = new Server();
        port = server.run(0);
        System.out.println("Started test HTTP server on " + port);
        facade = new ServerFacade("localhost:" + port);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    public void setup() {
        // make sure server data is cleared.
        facade.reset();

        // insert 1 existing user
        facade.register("existingUser", "password", "existinguser@gmail.com");

    }


    @Test
    public void sampleTest() {
        Assertions.assertTrue(true);
    }

    @Test
    public void testResetClearsData() {
        // Register a user
        facade.register("testUser", "testPass", "test@example.com");
        Assertions.assertTrue(facade.login("testUser", "testPass"), "User should exist before reset");

        // Reset the server
        facade.reset();

        // Check that the user is gone
        Assertions.assertFalse(facade.login("testUser", "testPass"), "User should not exist after reset");
    }

    @Test
    @Order(1)
    public void validRegisterUser() {
        // this returns a boolean value if the register is successful
        boolean register = facade.register("newUser", "password", "newuser@gmail.com");

        // check if the user is in the server
        Assertions.assertTrue(register, "Registration should succeed.");
    }

    @Test
    @Order(2)
    public void invalidRegisterUser() {
        // this returns a boolean value if the register is successful
        boolean register = facade.register("existingUser", "password", "newuser@gmail.com");

        // check if the user is in the server
        Assertions.assertFalse(register, "Registration should not succeed.");
    }

    @Test
    @Order(3)
    public void validLoginUser() {
        // this returns a boolean value if the login is successful
        boolean login = facade.login("existingUser", "password");

        // check if the login worked
        Assertions.assertTrue(login, "Login should be VALID.");
    }

    @Test
    @Order(4)
    public void invalidLoginUser() {
        // this returns a boolean value if the login was unsuccessful
        boolean login = facade.login("fakeUser", "password");

        // check for the invalid login
        Assertions.assertFalse(login, "Login should be INVALID.");
    }

    @Test
    @Order(5)
    public void validLogoutUser() {
        // this returns a boolean value if the logout was successful
        // remember, one user is already in the server
        boolean logout = facade.logout();

        // check for the invalid login
        Assertions.assertTrue(logout, "Logout should be VALID.");
    }

    @Test
    @Order(6)
    public void invalidLogoutUser() {
        // this returns a boolean value if the logout was successful
        // remember, one user is already in the server

        boolean logout = facade.logout();
        Assertions.assertTrue(logout, "First logout should be VALID.");

        boolean secondLogout = facade.logout();
        // check for the invalid login
        Assertions.assertFalse(secondLogout, "Second logout should be INVALID.");
    }

    @Test
    @Order(7)
    public void validCreateGame() {
        var gameID = facade.createGame("testGame");
        Assertions.assertTrue(gameID > 0, "Game ID should be greater than 0.");
    }

    @Test
    @Order(8)
    public void invalidCreateGame() {
        var gameID = facade.createGame("testGame");
        Assertions.assertTrue(gameID > 0, "Game ID should be greater than 0.");

        try {
            // try to create game with null name.
            facade.createGame(null);
        } catch (NullPointerException e) {
            Assertions.assertTrue(true, "Game name should not be null.");
        }
    }

    @Test
    @Order(9)
    public void validListGames() {
        // create some games
        var firstGame = facade.createGame("firstGame");
        Assertions.assertTrue(firstGame > 0, "first game ID should be greater than 0.");

        var secondGame = facade.createGame("secondGame");
        Assertions.assertTrue(secondGame > 1, "second game ID should be greater than 1.");

        var thirdGame = facade.createGame("thirdGame");
        Assertions.assertTrue(thirdGame > 2, "third game ID should be greater than 2.");

        Collection<GameData> games = facade.listGames();
        Assertions.assertNotNull(games, "games should not be null.");
        Assertions.assertEquals(3, games.size(), "games size should be 3.");

        // make sure game data has stayed the same
        // Check that firstGame, secondGame, and thirdGame are maintained
        boolean foundFirst = games.stream().anyMatch(game ->
                game.gameID() == firstGame && "firstGame".equals(game.gameName())
        );
        boolean foundSecond = games.stream().anyMatch(game ->
                game.gameID() == secondGame && "secondGame".equals(game.gameName())
        );
        boolean foundThird = games.stream().anyMatch(game ->
                game.gameID() == thirdGame && "thirdGame".equals(game.gameName())
        );

        Assertions.assertTrue(foundFirst && foundSecond && foundThird, "all game names should have been found.");
    }

    @Test
    @Order(10)
    public void invalidListGames() {
        // list empty games
        Collection<GameData> games = facade.listGames();
        Assertions.assertNotNull(games, "games should not be null.");
        Assertions.assertEquals(0, games.size(), "games size should be 0.");
    }

    @Test
    @Order(11)
    public void validJoinGame() {
        // create a game
        facade.login("existingUser", "password");
        var firstGame = facade.createGame("firstGame");
        Assertions.assertTrue(firstGame > 0, "first game ID should be greater than 0.");
        System.out.print("Game has been created.");

        boolean validJoinWhite = facade.joinGame(firstGame, "WHITE");

        // switch to a different user
        facade.register("newUser", "password", "newuser@gmail.com");
        facade.login("newUser", "password");

        // join the existing game
        boolean validJoinBlack = facade.joinGame(firstGame, "BLACK");

        // check if the joins were successful.
        Assertions.assertTrue(validJoinWhite && validJoinBlack);

        // note - the same user cannot join the same game twice, because it will throw auth token errors. two distinct users must join a game.
    }

    @Test
    @Order(11)
    public void invalidJoinGame() {
        // tries to join game twice as same user.
        var firstGame = facade.createGame("firstGame");
        Assertions.assertTrue(firstGame > 0, "first game ID should be greater than 0.");
        System.out.print("Game has been created.");

        boolean validJoinWhite = facade.joinGame(firstGame, "WHITE");
        Assertions.assertTrue(validJoinWhite);

        boolean invalidDuplicateJoin = facade.joinGame(firstGame, "WHITE");
        Assertions.assertFalse(invalidDuplicateJoin, "player cannot join game again as different team color");
    }
}
