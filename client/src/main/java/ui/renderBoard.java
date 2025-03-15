package ui;
import chess.ChessBoard;
import chess.ChessGame;
import static ui.EscapeSequences.*;

public class renderBoard {
    public static void printBoard(ChessGame game,  boolean whitePerspective) {
        ChessBoard board = game.getBoard();
        StringBuilder builder = new StringBuilder();
        builder.append(ERASE_SCREEN);

        // create indices for rows, and easy way to flip for perspective

        // build the header row and add columns labels

        // build the rows, set alternating colors and add pieces?

        // create helper function for getting the piece symbols.
    }
}
