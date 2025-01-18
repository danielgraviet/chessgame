package chess.MoveCalculations;

import chess.*;
import java.util.Collection;
import java.util.HashSet;

public class CalculateBishopMoves implements PieceMoveCalculator {

    public static Collection<ChessMove> getMoves(ChessBoard board, ChessPosition position) {
        int currentRow = position.getRow();
        int currentCol = position.getColumn();

        int[][] bishopMoves = {{1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
        HashSet<ChessMove> moves = new HashSet<>();

        for (int[] move : bishopMoves) {
            int newRow = currentRow;
            int newCol = currentCol;

            while (true) {
                newRow += move[0];
                newCol += move[1];

                ChessPosition newPosition = new ChessPosition(newRow, newCol);
                // first check move is on the board.
                if (!PieceMoveCalculator.isOnBoard(newPosition)) {
                    break;
                }

                ChessPiece chessPieceAtNewPosition = board.getPiece(newPosition);
                // test for case if piece is blocking the move.
                if (chessPieceAtNewPosition != null){
                    // checks if enemy team
                    if (chessPieceAtNewPosition.getTeamColor() != board.getPiece(position).getTeamColor()) {
                        // if enemy team, add it can capture that spot, then break.
                        moves.add(new ChessMove(position, newPosition, null));
                    }
                    // if same team, cannot move past, do not add.
                    break;
                }
                // if it is null(empty space), no issue, add to possible move
                moves.add(new ChessMove(position, newPosition, null));
            }
        }
        return moves;
    }
}
