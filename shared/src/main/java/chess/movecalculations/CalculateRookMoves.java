package chess.movecalculations;
import chess.*;

import java.util.Collection;


public class CalculateRookMoves {
    public static Collection<ChessMove> getMoves(ChessBoard board, ChessPosition position) {
        int[][] rookMoves = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        return PieceMoveCalculator.slidingPieces(board, position, rookMoves);
    }
}
