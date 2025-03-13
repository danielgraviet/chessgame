package client;

import model.auth.AuthData;
import org.junit.jupiter.api.*;
import server.Server;

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
}
