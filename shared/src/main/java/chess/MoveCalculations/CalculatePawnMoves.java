package chess.MoveCalculations;

import chess.ChessBoard;
import chess.ChessPosition;
import chess.*;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashSet;

public class CalculatePawnMoves {
    public static Collection<ChessMove> calculateMoves(ChessBoard board, ChessPosition position) {
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

        return moves;
    }
}