package server;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dataaccess.DataAccessException;
import model.auth.AuthData;
import model.users.EmptyResponse;
import model.users.UserData;
import spark.Request;
import spark.Response;
import service.UserService;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.xml.crypto.Data;

public class UserHandler {

    private final UserService userService;

    public UserHandler(UserService userService) {
        this.userService = userService;
    }

    public Object login(Request req, Response res) {
        AuthData authData;
        try {
            UserData userData = new Gson().fromJson(req.body(), UserData.class);
            authData = userService.login(userData);
            System.out.println("Login success: " + authData);

            res.status(200);
            return new Gson().toJson(authData);
        } catch (DataAccessException e){
            System.out.println("Login failed: " + e.getMessage());
            if (e.getMessage().contains("Username and password do not match.") || e.getMessage().contains("User not found.")){
                res.status(401);
                return createErrorResponse("Error: unauthorized");
            } else {
                res.status(500);
                return createErrorResponse("Error: " + e.getMessage());
            }
        }
    }

    public Object logout(Request req, Response res) {
        // not sure if this even works?

        try {
            // questions to ask?
            // what do I need to logout?
            String authData = req.headers("Authorization");
            System.out.println(authData);

            // study headers,
            //
            userService.logout(authData);
            res.status(200);
            return new Gson().toJson(new EmptyResponse());
        } catch (DataAccessException e) {

            if (e.getMessage().contains("Authorization is not valid.")) {
                res.status(401);
                return createErrorResponse("Error: unauthorized");

            } else {
                res.status(500);
                return createErrorResponse("Error: " + e.getMessage());
            }
        }
    }

    public Object register(Request req, Response res) {
        UserData userData = new Gson().fromJson(req.body(), UserData.class);
        AuthData authData;
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