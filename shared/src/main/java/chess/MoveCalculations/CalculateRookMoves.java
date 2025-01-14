package chess.MoveCalculations;
import chess.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;


public class CalculateRookMoves {
    public static Collection<ChessMove> calculateMoves(ChessBoard board, ChessPosition position) {
        HashSet<ChessMove> moves = new HashSet<>();
        int currentRow = position.getRow();
        int currentColumn = position.getColumn();

        int[][] rookMoves = {{1,0}, {-1,0}, {0,1}, {0,-1}};

        for (int[] rookMove : rookMoves) {
            int newRow = currentRow;
            int newColumn = currentColumn;

            while (true) {
                newRow += rookMove[0];
                newColumn += rookMove[1];

                ChessPosition newPosition = new ChessPosition(newRow, newColumn);

                if(!MoveHelp.isOnBoard(newPosition)){
                    break;
                }

                ChessPiece chessPieceAtNewPosition = board.getPiece(newPosition);

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
