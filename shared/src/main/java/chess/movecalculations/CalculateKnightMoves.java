package chess.movecalculations;

import chess.ChessBoard;
import chess.ChessPosition;
import chess.*;

import java.util.Collection;

public class CalculateKnightMoves implements PieceMoveCalculator {
    public static Collection<ChessMove> getMoves(ChessBoard board, ChessPosition position){
        int[][] knightMoves = {{2,1}, {1,2}, {-1,2}, {-2,1}, {-2,-1}, {-1,-2}, {1,-2}, {2,-1}};
        return PieceMoveCalculator.singleSpacePieces(board, position, knightMoves);
    }
}
