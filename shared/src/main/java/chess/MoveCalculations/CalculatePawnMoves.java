package chess.MoveCalculations;

import chess.ChessBoard;
import chess.ChessPosition;
import chess.*;

import java.util.Collection;
import java.util.HashSet;

public class CalculatePawnMoves {
    public static Collection<ChessMove> calculateMoves(ChessBoard board, ChessPosition position) {

        HashSet<ChessMove> moves = new HashSet<>();
        int currentRow = position.getRow();
        int currentColumn = position.getColumn();

        ChessPosition newPosition = new ChessPosition(currentRow, currentColumn);

        ChessGame.TeamColor teamColor = board.getPiece(newPosition).getTeamColor(); // what does this return ? boolean, string how can I check if it is white.
        // this should show moves if a pawn is white, and in starting position.
        if (teamColor == ChessGame.TeamColor.WHITE && currentRow == 2) {
            for (int i = 0; i < 2; i++) {
                currentRow += 1;
                newPosition = new ChessPosition(currentRow, currentColumn);
                if (board.getPiece(newPosition) == null) {
                    moves.add(new ChessMove(position, newPosition, null));
                }
            }
        } else if (teamColor == ChessGame.TeamColor.BLACK && currentRow == 7) { // calculates moves for black piece.
            for (int i = 0; i < 2; i++) {
                currentRow -= 1;
                newPosition = new ChessPosition(currentRow, currentColumn);
                if (board.getPiece(newPosition) == null) {
                    moves.add(new ChessMove(position, newPosition, null));
                } else {
                    break;
                }
            }
        } else if (teamColor == ChessGame.TeamColor.WHITE && currentRow > 2) {
            // check if anything is in front.
            for (int i = 0; i < 1; i++) {
                currentRow += 1;
                newPosition = new ChessPosition(currentRow, currentColumn);
                if (board.getPiece(newPosition) == null) {
                    moves.add(new ChessMove(position, newPosition, null));
                } else {
                    break;
                }
            }
        } else if (teamColor == ChessGame.TeamColor.BLACK && currentRow < 7) {
            for (int i = 0; i < 1; i++) {
                currentRow -= 1;
                newPosition = new ChessPosition(currentRow, currentColumn);
                if (board.getPiece(newPosition) == null) {
                    moves.add(new ChessMove(position, newPosition, null));
                }
            }


            // pawn in middle of board, nothing block. white and black
            // if white pawn can moves one spot forward (row += 1).
            // else (meaning black) pawn moves one post back (row -=1)

            // pawn initial move for white and black.

            // pawn promotion for white and blacks
        }
        return moves;
    }
}
