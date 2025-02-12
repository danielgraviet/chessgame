package server;

import dataaccess.*;
import spark.*;
import service.UserService;
import server.UserHandler;



public class Server {
    UserHandler userServer;
    static UserService userService;
    UserDAO userDAO;
    AuthDAO authDAO;

    public Server() {
        this.userDAO = new MemoryUserDAO();
        this.authDAO = new MemoryAuthDAO();
        userService = new UserService(userDAO, authDAO);
        this.userServer = new UserHandler(userService);
    }

    public int run(int desiredPort) {
        Spark.port(desiredPort);

        Spark.staticFiles.location("web");

        // Register your endpoints and handle exceptions here.
        Spark.post("/user", userServer::register);
        Spark.post("/session", userServer::login);
        Spark.delete("/db", this::clear);
        Spark.delete("/session", userServer::logout);

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
