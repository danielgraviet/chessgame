package client;

import chess.ChessGame;
import com.google.gson.Gson;
import model.game.GameData;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class HttpCommunicator implements ServerCommunicator {
    private final String baseUrl;
    private final ServerFacade facade;
    private static final Gson GSON = new Gson();

    // this class is like a mailman, and takes care of all the tasks delivering and receiving
    public HttpCommunicator(ServerFacade facade,  String serverName) {
        // 1. Handle null input first
        if (serverName == null) {
            throw new IllegalArgumentException("Server URL cannot be null");
        }

        // 2. Use a temporary variable to hold the potentially modified URL
        String processedUrl = serverName;

        // 3. Modify the temporary variable if needed
        if (processedUrl.endsWith("/")) {
            processedUrl = processedUrl.substring(0, processedUrl.length() - 1);
        }

        // 4. Assign the final, processed value *ONCE* to the final field
        this.baseUrl = processedUrl;

        // 5. Assign other final fields
        this.facade = facade;
    }

    public boolean register(String username, String password, String email) {
        // think of this as a python dictionary.
        System.out.println("DEBUG COMM [REGISTER] Called with: user=" + username); // Debug start
        Map<String, String> body = Map.of("username", username, "password", password, "email", email);
        Map<String, Object> response = sendRequest("POST", "/user", body);
        System.out.println("DEBUG COMM [REGISTER] Raw response received: " + response);
        return handleAuthResponse(response);
    }


    public boolean login(String username, String password) {
        System.out.println("DEBUG COMM [LOGIN] Called with: user=" + username);
        Map<String, String> body = Map.of("username", username, "password", password);
        Map<String, Object> response = sendRequest("POST", "/session", body);
        System.out.println("DEBUG COMM [LOGIN] Raw response received: " + response);
        return handleAuthResponse(response);
    }

    public boolean logout(){
        System.out.println("DEBUG COMM [LOGOUT] Called"); // Debug start
        // this should log out the user, and remove the auth token
        Map<String, Object> response = sendRequest("DELETE", "/session", null);
        System.out.println("DEBUG COMM [LOGOUT] Raw response received: " + response);
        return handleAuthResponse(response);
    }

    public void reset() {
        System.out.println("DEBUG COMM [RESET] Called"); // Debug start
        Map<String, Object> response = sendRequest("DELETE", "/db", null);
        if (response.containsKey("error")) {
            System.err.println("ERROR COMM [RESET] Failed: " + response.get("error"));
            throw new RuntimeException("Failed te reset server: " + response);
        }
        System.out.println("System Reset: " + response);
    }

    public int createGame(String gameName) {
        System.out.println("DEBUG COMM [CREATE_GAME] Called with: name=" + gameName); // Debug start
        Map<String, Object> response = sendRequest("POST", "/game", Map.of("gameName", gameName));

        // simple error checking
        if (response.containsKey("error")) {
            throw new RuntimeException("Failed te create game: " + response);
        }

        // check for an existing id
        if (!response.containsKey("gameID")) {
            throw new RuntimeException("Missing gameID: " + response);
        }

        // get the gameID and convert it to an int
        Object gameID = response.get("gameID");
        if (gameID == null) {
            throw new RuntimeException("gameID is null: " + response);
        } else if (gameID instanceof Number) {
            return ((Number) gameID).intValue();
        } else {

            throw new RuntimeException("gameID is not an integer: " + response);
        }
    }

    public HashSet<GameData> listGames() {
        //Spark.get("/game", gameServer::listGames);
        Map<String, Object> response = sendRequest("GET", "/game", null);

        // error checking
        if (response.containsKey("error")) {
            String errorMsg = response.containsKey("message") ? (String) response.get("message") : "Unknown error";
            throw new RuntimeException("Failed to list games: " + response.get("error") + " - " + errorMsg);
        }

        // make sure games are present
        Object gamesObject = response.get("games");
        if (gamesObject == null) {
            throw new RuntimeException("Missing games: " + response);
        }

        // storage for rawGames (json) -> games (gameData objects)
        List<?> rawGamesList = (List<?>) gamesObject;
        HashSet<GameData> games = new HashSet<>();

        for (Object game : rawGamesList) {
            if (!(game instanceof Map)) {
                throw new RuntimeException("Invalid game format: " + game);
            }

            Map<String, Object> gameMap = (Map<String, Object>) game;

            // extract the objects and prepare for conversion
            Object gameIDObject = gameMap.get("gameID");
            Object whiteUsernameObject = gameMap.get("whiteUsername");
            Object blackUsernameObject  = gameMap.get("blackUsername");
            Object gameNameObject = gameMap.get("gameName");

            // convert from number to int.
            int gameID = ((Number) gameIDObject).intValue();

            // make sure null is also okay.
            String whiteUsername = whiteUsernameObject != null ? whiteUsernameObject.toString() : null;
            String blackUsername = blackUsernameObject != null ? blackUsernameObject.toString() : null;

            String gameName = gameNameObject.toString();
            ChessGame chessGame = null;
            Object chessGameObject = gameMap.get("game");
            if (chessGameObject != null) {
                if (chessGameObject instanceof String) {
                    // System.out.println("Chess Game is instance of String: " + chessGameObject);
                    chessGame = GSON.fromJson((String) chessGameObject, ChessGame.class);
                } else if (chessGameObject instanceof Map) {
                    // System.out.println("Chess Game is instance of Map: " + chessGameObject);
                    String chessGameJson = GSON.toJson(chessGameObject);
                    chessGame = GSON.fromJson(chessGameJson, ChessGame.class);
                }
            } else {
                System.out.println("Chess Game object is null: " + gameMap);
            }

            games.add(new GameData(gameID, whiteUsername, blackUsername, gameName, chessGame));
        }

        return games;
    }

    public boolean joinGame(int gameID, String playerColor) {
        //Spark.put("/game", gameServer::joinGame);
        System.out.println("DEBUG COMM [JOIN_GAME] Called with: gameID=" + gameID + ", color=" + playerColor);
        Map<String, Object> body = Map.of("gameID", gameID, "playerColor", playerColor);
        Map<String, Object> response = sendRequest("PUT", "/game", body);
        // signify if it worked.
        return handleAuthResponse(response);
    }
    // private methods
    private Map<String, Object> sendRequest(String method, String endpoint, Map<String, ?> body) {
        System.out.println("---"); // Separator for clarity
        System.out.println("DEBUG COMM [SEND] >> Method: " + method);
        System.out.println("DEBUG COMM [SEND] >> Endpoint: " + endpoint);
        System.out.println("DEBUG COMM [SEND] >> Body: " + (body == null ? "null" : GSON.toJson(body)));
        System.out.println("DEBUG COMM [SEND] >> Auth Token Used: " + facade.getAuthToken());
        HttpURLConnection connection = null;
        Map<String, Object> responseMap = null;
        try {
            connection = setupConnection(method, endpoint, body);
            int status = connection.getResponseCode(); // Execute the request
            System.out.println("DEBUG COMM [RECV] << Status: " + status); // Log the actual status code

            InputStream responseStream = null;
            // Check if the status code indicates success (usually 2xx)
            if (status >= 200 && status < 300) {
                responseStream = connection.getInputStream();
            } else {
                // If status code indicates an error (4xx or 5xx), use the error stream
                responseStream = connection.getErrorStream();
            }

            // Try to read and parse the response body from the appropriate stream
            if (responseStream != null) {
                try (var reader = new InputStreamReader(responseStream)) {
                    // Use a more flexible type if responses aren't always Maps
                    responseMap = GSON.fromJson(reader, Map.class);
                } catch (Exception parseEx) {
                    // Handle cases where the response body isn't valid JSON or isn't a Map
                    System.err.println("ERROR COMM [RECV] Failed to parse response body: " + parseEx.getMessage());
                    // Create an error map even if parsing fails, including the status
                    responseMap = Map.of("error", "Failed to parse response (Status: " + status + ")", "status", status);
                }
            }

            // If after all that, responseMap is still null (e.g., empty 204 No Content, or stream error), create a map
            if (responseMap == null) {
                if (status >= 200 && status < 300) {
                    // Success status but no body / parse error
                    responseMap = Map.of("status", status); // Indicate success status
                } else {
                    // Error status and couldn't read/parse body
                    responseMap = Map.of("error", "Server returned status " + status + " with no parseable body", "status", status);
                }
            }

            // If it was an error status, ensure the map contains an "error" key for handleAuthResponse
            if (status >= 400 && !responseMap.containsKey("error") && responseMap.containsKey("message")) {
                // If server sent {"message": "..."}, promote it to "error" for client logic
                responseMap = new java.util.HashMap<>(responseMap); // Make mutable copy
                responseMap.put("error", responseMap.get("message"));
            } else if (status >= 400 && !responseMap.containsKey("error")) {
                // Ensure an error key exists if it's an error status but no message/error key was parsed
                responseMap = new java.util.HashMap<>(responseMap); // Make mutable copy
                responseMap.put("error", "HTTP Error: " + status);
            }


            return responseMap;

        } catch (IOException e) {
            // Network errors, connection refused, DNS issues etc.
            System.err.println("ERROR COMM [SEND] Network/Connection IOException: " + e.getMessage());
            return Map.of("error", "Connection error: " + e.getMessage()); // More specific than just "http"
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private HttpURLConnection setupConnection(String method, String endpoint, Map<String, ?> body) throws IOException {
        URI uri = URI.create(baseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod(method);

        //String authToken = facade.getAuthToken();
        //System.out.println("Auth token for " + method + " " + endpoint + " " + (authToken != null ? authToken : "null"));


        if (facade.getAuthToken() != null) {
            conn.setRequestProperty("Authorization", facade.getAuthToken());
        }

        if (body != null) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            try (var output = conn.getOutputStream()) {
                output.write(GSON.toJson(body).getBytes());
            }
        }
        return conn;
    }

    private boolean handleAuthResponse(Map<String, Object> response) {
        if (response.containsKey("error")) {
            return false;
        }

        if (response.containsKey("authToken") && response.get("authToken") != null) {
            facade.setAuthToken((String) response.get("authToken"));
        }
        return true;
    }
}
