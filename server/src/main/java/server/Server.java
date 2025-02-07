package server;

import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.UserDAO;
import spark.*;
import service.UserService;

import javax.xml.crypto.Data;
import java.sql.SQLException;

public class Server {
    UserHandler userServer;
    static UserService userService;
    UserDAO userDAO;
    AuthDAO authDAO;

    public Server() {
        userService = new UserService(userDAO, authDAO);
        this.userServer = new UserHandler(userService);
    }

    public int run(int desiredPort) {
        Spark.port(desiredPort);

        Spark.staticFiles.location("web");

        // Register your endpoints and handle exceptions here.
        Spark.post("/user", userServer::register);
        Spark.post("/login", userServer::login);
        Spark.delete("/db", this::clear);
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
