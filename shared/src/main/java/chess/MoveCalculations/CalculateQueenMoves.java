package chess.MoveCalculations;

import chess.ChessBoard;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;

import java.util.Collection;
import java.util.HashSet;

public class CalculateQueenMoves {
    public static Collection<ChessMove> calculateMoves(ChessBoard board, ChessPosition position){
        int currentRow = position.getRow();
        int currentCol = position.getColumn();

        int[][] queenMoves = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
        HashSet<ChessMove> moves = new HashSet<>();
        for (int[] move : queenMoves) {
            int newRow = currentRow;
            int newCol = currentCol;

            while (true) {
                newRow += move[0];
                newCol += move[1];

                ChessPosition newPosition = new ChessPosition(newRow, newCol);

                if (!MoveHelp.isOnBoard(newPosition)){
                    break;
                }

                ChessPiece chessPieceAtNewPosition = board.getPiece(newPosition);

                // replace this with function call to move help.
                if (chessPieceAtNewPosition != null){
                    if (chessPieceAtNewPosition.getTeamColor() != board.getPiece(position).getTeamColor()){
                        moves.add(new ChessMove(position, newPosition, null));
                    }
                    break;
                }
                moves.add(new ChessMove(position, newPosition, null));
            }

        }
        return moves;
    }
}
