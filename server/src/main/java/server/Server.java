package server;

import dataaccess.*;
import service.GameService;
import spark.*;
import service.UserService;
import server.UserHandler;
import server.GameHandler;



public class Server {
    UserHandler userServer;
    GameHandler gameServer;


    static UserService userService;
    static GameService gameService;

    UserDAO userDAO;
    AuthDAO authDAO;
    GameDAO gameDAO;

    public Server() {
        // this makes all the new objects to store users, auth tokens, and games.
        this.userDAO = new MemoryUserDAO();
        this.authDAO = new MemoryAuthDAO();
        this.gameDAO = new MemoryGameDAO();

        userService = new UserService(userDAO, authDAO);

        gameServer = new GameHandler(gameService);
        gameService = new GameService(gameDAO, authDAO);

        this.userServer = new UserHandler(userService);
        this.gameServer = new GameHandler(gameService);
    }

    public int run(int desiredPort) {
        Spark.port(desiredPort);

        Spark.staticFiles.location("web");

        // Register your endpoints and handle exceptions here.
        Spark.post("/user", userServer::register);
        Spark.post("/session", userServer::login);
        Spark.delete("/db", this::clear);
        Spark.delete("/session", userServer::logout);

        // games
        Spark.post("/game", gameServer::createGame);
        Spark.put("/game", gameServer::joinGame);


        Spark.awaitInitialization();
        return Spark.port();
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }

    private Object clear(Request req, Response res) throws DataAccessException {
        clearDatabase();
        res.status(200);
        return "{}";
    }

    public void clearDatabase() throws DataAccessException {
        userService.clear();
    }
}
