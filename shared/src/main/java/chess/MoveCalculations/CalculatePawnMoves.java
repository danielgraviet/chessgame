package chess.MoveCalculations;

import chess.ChessBoard;
import chess.ChessPosition;
import chess.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class CalculatePawnMoves {
    public static Collection<ChessMove> calculateMoves(ChessBoard board, ChessPosition position) {

        HashSet<ChessMove> moves = new HashSet<>();
        int currentRow = position.getRow();
        int currentColumn = position.getColumn();

        int[][] whitePawnBasicMove = {{1,0}};
        int[][] blackPawnBasicMove = {{-1,0}};


        // pawn in middle of board, nothing block. white and black
        // if white pawn can moves one spot forward (row += 1).
        // else (meaning black) pawn moves one post back (row -=1)

        // pawn initial move for white and black.

        // pawn promotion for white and black.
        return null;
    }
}
