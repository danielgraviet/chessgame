package chess.MoveCalculations;

import chess.ChessBoard;
import chess.ChessPosition;
import chess.*;

import java.util.Collection;
import java.util.HashSet;

public class CalculateKnightMoves {
    public static Collection<ChessMove> calculateMoves(ChessBoard board, ChessPosition position){
        HashSet<ChessMove> moves = new HashSet<>();
        int currentRow = position.getRow();
        int currentColumn = position.getColumn();

        int[][] knightMoves = {{2,1}, {1,2}, {-1,2}, {-2,1}, {-2,-1}, {-1,-2}, {1,-2}, {2,-1}};

        for (int[] move : knightMoves) {
            int newRow = currentRow;
            int newColumn = currentColumn;

            for (int i = 0; i < 1; i++){
                newRow += move[0];
                newColumn += move[1];

                ChessPosition newPosition = new ChessPosition(newRow, newColumn);

                if(!MoveHelp.isOnBoard(newPosition)){
                    break;
                }

                ChessPiece chessPieceAtNewPostition = board.getPiece(newPosition);

                if (chessPieceAtNewPostition != null) {
                    if (chessPieceAtNewPostition.getTeamColor() != board.getPiece(position).getTeamColor()) {
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
