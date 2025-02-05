package chess.movecalculations;

import chess.*;
import java.util.Collection;

public class CalculateBishopMoves implements PieceMoveCalculator {

    public static Collection<ChessMove> getMoves(ChessBoard board, ChessPosition position) {
        int[][] bishopMoves = {{1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
        return PieceMoveCalculator.slidingPieces(board, position, bishopMoves);
    }
}
