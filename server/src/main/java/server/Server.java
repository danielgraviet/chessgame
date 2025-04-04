package server;

import dataaccess.*;
import server.wshandlerfunctions.*;
import service.GameService;
import spark.*;
import service.UserService;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.jetty.websocket.api.Session;



public class Server {
    UserHandler userServer;
    GameHandler gameServer;

    static UserService userService;
    static GameService gameService;

    UserDAO userDAO;
    AuthDAO authDAO;
    GameDAO gameDAO;
    ConnectionManager connectionManager;
    HelperFunctions helperFunctions;

    // WS Handlers
    HandleConnect handleConnect;
    HandleMakeMove handleMakeMove;
    HandleLeave handleLeave;
    HandleResign handleResign;

    static ConcurrentHashMap<Integer, List<Session>> sessions = new ConcurrentHashMap<>();

    public Server() {
        // this makes all the new objects to store users, auth tokens, and games.
        try {
            DatabaseManager.createDatabase();
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to create database: " + e.getMessage());
        }

        // this implements the sql database
        this.userDAO = new SqlUserDAO();
        this.authDAO = new SqlAuthDAO();
        this.gameDAO = new SqlGameDAO();
        this.connectionManager = new ConnectionManager();
        this.helperFunctions = new HelperFunctions(this.connectionManager);


        userService = new UserService(userDAO, authDAO);
        gameService = new GameService(gameDAO, authDAO);
        gameServer = new GameHandler(gameService);

        this.userServer = new UserHandler(userService);
        this.gameServer = new GameHandler(gameService);

        this.handleConnect = new HandleConnect(gameService, authDAO, gameDAO, connectionManager, helperFunctions);
        this.handleMakeMove = new HandleMakeMove(gameService, authDAO, gameDAO, connectionManager, helperFunctions);
        this.handleLeave = new HandleLeave(gameService, authDAO, gameDAO, connectionManager, helperFunctions);
        this.handleResign = new HandleResign(gameService, authDAO, gameDAO, connectionManager, helperFunctions);
    }

    public int run(int desiredPort) {
        Spark.port(desiredPort);

        Spark.staticFiles.location("web");

        WSHandler.setAuthDAO(this.authDAO);
        WSHandler.setGameDAO(this.gameDAO);
        WSHandler.setGameService(gameService);
        WSHandler.setConnectionManager(this.connectionManager);
        WSHandler.setHandleConnect(this.handleConnect);
        WSHandler.setHandleMakeMove(this.handleMakeMove);
        WSHandler.setHandleLeave(this.handleLeave);
        WSHandler.setHandleResign(this.handleResign);
        WSHandler.setHelperFunctions(this.helperFunctions);

        // Websocket
        Spark.webSocket("/ws", WSHandler.class);

        // Register your endpoints and handle exceptions here.
        Spark.post("/user", userServer::register);
        Spark.post("/session", userServer::login);
        Spark.delete("/db", this::clear);
        Spark.delete("/session", userServer::logout);

        // games
        Spark.post("/game", gameServer::createGame);
        Spark.put("/game", gameServer::joinGame);
        Spark.get("/game", gameServer::listGames);


        Spark.awaitInitialization();
        return Spark.port();
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }

    private Object clear(Request req, Response res) throws DataAccessException {
        // issue resolved, I was clearing auth and users, not the game storage. caused leaks. dg/2.17
        clearDatabase();
        // fix this in the future. not sure if this is necessary.
        sessions.clear();
        res.status(200);
        return "{}";
    }

    public void clearDatabase() throws DataAccessException {
        userService.clear();
        gameService.clear();
    }
}
