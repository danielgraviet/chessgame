package chess.MoveCalculations;

import chess.*;
import java.util.Collection;
import java.util.HashSet;

public class CalculateKingMoves implements PieceMoveCalculator {
    public static Collection<ChessMove> getMoves(ChessBoard board, ChessPosition position) {
        int[][] kingMoves = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {    -1, -1}, {1, -1}, {-1, 1}};
        return PieceMoveCalculator.singleSpacePieces(board, position, kingMoves);
    }
}
