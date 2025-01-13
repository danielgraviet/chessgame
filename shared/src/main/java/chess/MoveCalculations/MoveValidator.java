package chess.MoveCalculations;

import chess.ChessBoard;
import chess.ChessMove;
import chess.ChessPosition;

public class MoveValidator {
    public static boolean isValidMove(ChessPosition position) {
        int row = position.getRow();
        int col = position.getColumn();

        return row <= 8 && row >= 1 && col <= 8 && col >= 1;
    }
}
