package client;

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
        // make sure server data is cleared. might need a reset method.
        facade.reset();
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

}
