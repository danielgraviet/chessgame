package server;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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

    public Object register(Request req, Response res) {
        UserData userData = new Gson().fromJson(req.body(), UserData.class);
        AuthData authData = null;
        try {
            authData = userService.register(userData);

            // handles errors.
            if (userData.username() == null || userData.password() == null) {
                res.status(400);
                return createErrorResponse("Error: bad request");
            }

        } catch (DataAccessException e) {
            if (e.getMessage().equals("User already exists")) {
                res.status(403);
                return createErrorResponse("Error: already taken");
            }

            res.status(500);
            return createErrorResponse("Error: " + e.getMessage());
            // store string inside object, and then return it as a gson.
        }
        res.status(200);
        return new Gson().toJson(authData);
    }

    private String createErrorResponse(String error) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("message", error);
        return new Gson().toJson(jsonObject);
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