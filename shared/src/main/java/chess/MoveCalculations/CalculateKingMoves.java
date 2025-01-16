package chess.MoveCalculations;

import chess.*;
import java.util.Collection;
import java.util.HashSet;

public class CalculateKingMoves implements PieceMoveCalculator {
    public static Collection<ChessMove> getMoves(ChessBoard board, ChessPosition position) {
        int currentRow = position.getRow();
        int currentCol = position.getColumn();

        int[][] kingMoves = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
        HashSet<ChessMove> moves = new HashSet<>();

        for (int[] move : kingMoves) {
            int newRow = currentRow;
            int newCol = currentCol;

            // run once for each direction.
            for (int i = 0; i < 1; i++) {
                newRow += move[0];
                newCol += move[1];

                ChessPosition newPostition = new ChessPosition(newRow, newCol);

                if (PieceMoveCalculator.isOnBoard(newPostition)){
                    break;
                }

                ChessPiece chessPieceAtNewPosition = board.getPiece(newPostition);
                if (chessPieceAtNewPosition != null){
                    if (chessPieceAtNewPosition.getTeamColor() != board.getPiece(position).getTeamColor()){
                        moves.add(new ChessMove(position, newPostition, null));
                    }
                    break;
                }
                moves.add(new ChessMove(position, newPostition, null));
            }
        }
        return moves;
    }
}
