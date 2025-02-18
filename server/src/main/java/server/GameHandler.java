package server;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dataaccess.DataAccessException;
import service.GameService;
import model.users.EmptyResponse;
import model.game.JoinGameRequest;
import model.users.UserData;
import spark.Request;
import spark.Response;
import service.UserService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import chess.ChessGame;

public class GameHandler {
    private final GameService gameService;

    public GameHandler(GameService gameService) {
        this.gameService = gameService;
    }

    public Object createGame(Request req, Response res) {
        // this is where I set all my error codes.
        try {
            String authData = req.headers("Authorization");

            if (authData == null || authData.isEmpty()) {
                res.status(400);
                return createErrorResponse("Error: bad request");
            }

            // need a way to check that the authToken is in the database.

            int gameID = gameService.createGame(authData, req.body());
            res.status(200);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("gameID", gameID);
            return new Gson().toJson(responseMap);

        } catch (DataAccessException e) {
            if (e.getMessage().equals("Invalid token.")) {
                res.status(401);
                return createErrorResponse("Error: unauthorized");
            } else {
                res.status(500);
                return createErrorResponse("Error: " + e.getMessage());
            }
        }
    }

    public Object joinGame(Request req, Response res) {
        try {
            // make sure request has a valid auth token DG/2-15
            String authToken = req.headers("Authorization");
            if (authToken == null || authToken.isEmpty()) {
                res.status(400);
                return createErrorResponse("Error: Missing authorization token");
            }

            // use the JoinGameRequest model to get correct "playerColor" and "gameID"
            JoinGameRequest joinRequest;
            try {
                joinRequest = new Gson().fromJson(req.body(), JoinGameRequest.class);
            } catch (Exception e) {
                res.status(400);
                return createErrorResponse("Error: Invalid JSON in request body");
            }
            if (joinRequest == null) {
                res.status(400);
                return createErrorResponse("Error: Invalid join request");
            }

            // get the gameID and teamColor
            int gameID = joinRequest.gameID();
            String teamColorStr = joinRequest.playerColor();

            // teamColorStr is getting null, and throwing this error.
            // questions? Where is the team color being stored?
            if (teamColorStr == null || teamColorStr.isEmpty()) {
                res.status(400);
                return createErrorResponse("Error: Missing team color");
            }

            // because the TeamColor is an enum, (all caps), we have to convert it here.
            // this also prevents invalid team colors from being used.
            ChessGame.TeamColor teamColor;
            try {
                teamColor = ChessGame.TeamColor.valueOf(teamColorStr.toUpperCase());
            } catch (IllegalArgumentException ex) {
                res.status(400);
                return createErrorResponse("Error: Invalid team color");
            }

            // join the game
            gameService.joinGame(authToken, gameID, teamColor);

            res.status(200);
            return new Gson().toJson(new EmptyResponse());
        } catch (DataAccessException e) {

            String errorMessage = e.getMessage();

            if (errorMessage.equals("Invalid token.")) {
                res.status(401);
                return createErrorResponse("Error: unauthorized");

            } else if (errorMessage.contains("already")) {
                // This covers error messages where the white or black team already joined.
                res.status(403);
                return createErrorResponse("Error: team already taken");

            } else if (errorMessage.contains("Game not found")) {
                res.status(400);
                return createErrorResponse("Error: game not found");

            } else {
                res.status(500);
                return createErrorResponse("Error: " + errorMessage);
            }

        } catch (Exception e) {
            res.status(500);
            return createErrorResponse("Error: " + e.getMessage());
        }
    }

    public Object listGames(Request req, Response res) {
        try {
            // make sure there is a valid auth token.
            String authToken = req.headers("Authorization");
            if (authToken == null || authToken.isEmpty()) {
                res.status(400);
                return createErrorResponse("Invalid authorization.");
            }

            var games = gameService.getAllGames(authToken);
            Map<String, Object> response = new HashMap<>();
            response.put("games", games);
            res.status(200);
            return new Gson().toJson(response);
        } catch (Exception e) {
            res.status(500);
        }
        return new ArrayList<>();
    }

    private String createErrorResponse(String error) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("message", error);
        return new Gson().toJson(jsonObject);
    }
}
