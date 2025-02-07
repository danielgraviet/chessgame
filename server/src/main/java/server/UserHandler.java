package server;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import model.auth.AuthData;
import model.users.UserData;
import spark.Request;
import spark.Response;
import service.UserService;

public class UserHandler {

    UserService userService;

    public UserHandler(UserService userService) {
        this.userService = userService;
    }

    public Object login(Request req, Response res) throws DataAccessException {
        // send request with correct username and password?
        // turn my data object into the correct json format with GSON stuff.
        UserData userData = new Gson().fromJson(req.body(), UserData.class);
        AuthData authData = userService.login(userData);


        // this should give success response of 200
        res.status(200);
        // return a username and authToken response.
        // failure response of 401 unauthorized or 500 other errors.
        return authData;
    }

    public Object register(Request req, Response res) throws DataAccessException {
        UserData userData = new Gson().fromJson(req.body(), UserData.class);
        AuthData authData = userService.register(userData);

        res.status(200);

        return authData;
    }
}

//var serializer = new Gson();
//
//var game = new ChessGame();
//
//// serialize to JSON
//var json = serializer.toJson(game);
//
//// deserialize back to ChessGame
//game = serializer.fromJson(json, ChessGame.class);