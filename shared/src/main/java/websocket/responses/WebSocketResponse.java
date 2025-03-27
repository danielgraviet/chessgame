package websocket.responses;

public class WebSocketResponse {
    private String status;
    private Integer gameID;
    private String message;

    public WebSocketResponse(String status, Integer gameID, String message) {
        this.status = status;
        this.gameID = gameID;
        this.message = message;
    }

    // Getters for Gson serialization
    public String getStatus() {
        return status;
    }

    public Integer getGameID() {
        return gameID;
    }

    public String getMessage() {
        return message;
    }
}
