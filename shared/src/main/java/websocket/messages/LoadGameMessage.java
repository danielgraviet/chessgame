package websocket.messages;

import chess.ChessGame;
import model.game.GameData;

public class LoadGameMessage extends ServerMessage {
    private GameData game;

    public LoadGameMessage(GameData gameData) {
        super(ServerMessageType.LOAD_GAME);
        this.game = gameData;
    }

    public GameData getGame() {
        return game;
    }
}
