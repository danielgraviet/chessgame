package chess.movecalculations;

import chess.ChessBoard;
import chess.ChessMove;
import chess.ChessPosition;

import java.util.Collection;

public class CalculateQueenMoves implements PieceMoveCalculator {
    public static Collection<ChessMove> getMoves(ChessBoard board, ChessPosition position) {
        int[][] queenMoves = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
        return PieceMoveCalculator.slidingPieces(board, position, queenMoves);
    }
}
