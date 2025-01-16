package chess.MoveCalculations;

import chess.ChessBoard;
import chess.ChessPosition;
import chess.*;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashSet;

public class CalculatePawnMoves implements PieceMoveCalculator {
    public static Collection<ChessMove> getMoves(ChessBoard board, ChessPosition position) {
        HashSet<ChessMove> moves = new HashSet<>();
        int currentRow = position.getRow();
        int currentColumn = position.getColumn();
        ChessPiece currentPiece = board.getPiece(position);

        // check early for invalid piece
        if (currentPiece == null) {
            return moves;
        }

        ChessGame.TeamColor teamColor = currentPiece.getTeamColor();

        int direction = (teamColor == ChessGame.TeamColor.WHITE) ? 1 : -1;
        int startingRow = (teamColor == ChessGame.TeamColor.WHITE ? 2 : 7);

        // add the starting moves.
            // things to check for.
            // - move piece up 1, or down 1, according to color, and if there is no piece blocking it.
            // - if 1st move is successful, move again according to color, and if there is no piece blocking it.
        if (currentRow == startingRow) {
            moves.addAll(CalculatePawnMoves.startingMoves(board, position, direction));
        }



        // add the attack moves
            // things to check for
            // - in addition to the starting moves, check if an enemy piece is diagonal
                // white: (+1, +1), (+1, -1)
                // black: (-1, -1), (-1, +1)
            // - if it is, add those positions to the hashset.


        // finally return the full array.
        return moves;
    }

    public static Collection<ChessMove> startingMoves(ChessBoard board, ChessPosition position, int direction) {
        HashSet<ChessMove> startingMoves = new HashSet<>();
        int currentRow = position.getRow();
        int currentColumn = position.getColumn();
        int[][] pawnStartMoves = {{direction,0}, {direction * 2,0}};

        for (int[] move : pawnStartMoves) {
            int newRow = currentRow + move[0];
            int newColumn = currentColumn + move[1];

            ChessPosition newPosition = new ChessPosition(newRow, newColumn);
            if (!PieceMoveCalculator.isOnBoard(newPosition)){
                break;
            };
            ChessPiece newPiece = board.getPiece(newPosition);

            if (newPiece != null) {
                break;
            } else {
                startingMoves.add(new ChessMove(position, newPosition, null));
            }

        }
        return startingMoves;
    }
}