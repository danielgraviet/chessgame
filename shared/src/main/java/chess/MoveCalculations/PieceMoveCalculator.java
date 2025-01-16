package chess.MoveCalculations;
import chess.ChessBoard;
import chess.ChessMove;
import chess.ChessPosition;
import java.util.HashSet;

public interface PieceMoveCalculator {
    static HashSet<ChessMove> getMoves(ChessBoard board, ChessPosition position) {
        return null;
    }

    static boolean isOnBoard(ChessPosition position) {
        int currentRow = position.getRow();
        int currentColumn = position.getColumn();
        return currentRow <= 8 && currentRow >= 1 && currentColumn <= 8 && currentColumn >= 1;
    }
}
