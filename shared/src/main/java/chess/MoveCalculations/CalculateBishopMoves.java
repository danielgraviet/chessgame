package chess.MoveCalculations;

import chess.ChessMove;
import chess.ChessPosition;
import chess.ChessBoard;

import java.util.ArrayList;
import java.util.Collection;

public class CalculateBishopMoves {

    public static Collection<ChessMove> calculateMoves(ChessBoard board, ChessPosition position) {
        ArrayList<ChessMove> moves = new ArrayList<>();
        int currRow = position.getRow();
        int currColumn = position.getColumn();

        for(int i = 1; i < 8; i++) {
            int newRowPosition = currRow + 1;
            int newColumnPosition = currColumn + 1;
            ChessPosition newPosition = new ChessPosition(newRowPosition, newColumnPosition);

            moves.add(new ChessMove(position, newPosition, null));
        }
        return moves;
    }
}
