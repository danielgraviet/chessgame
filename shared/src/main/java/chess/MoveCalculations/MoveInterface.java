package chess.MoveCalculations;

import chess.ChessBoard;
import chess.ChessPosition;

import java.util.List;

public interface MoveInterface {
    boolean canMove(ChessPosition start, ChessPosition end, ChessBoard board);
    List<ChessPosition> getValidMoves(ChessPosition start, ChessPosition end, ChessBoard board);
}
