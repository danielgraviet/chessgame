package ui;

import chess.ChessGame;

public interface GameHandlerUI {
    void displayNotification(String message);
    void displayError(String message);
    void updateBoard(ChessGame game);
    void redrawPrompt();
}
