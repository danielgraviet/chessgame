package chess.MoveCalculations;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;
import chess.MoveCalculations.MoveInterface;


public class MoveHelp {
    public static boolean canMove(ChessPosition start, ChessPosition end, ChessBoard board) {
        return false;
    }

    public static boolean isOnBoard(ChessPosition position) {
        int row = position.getRow();
        int col = position.getColumn();
        return row >= 1 && row <= 8 && col >= 1 && col <= 8;
    }

    public static boolean isTeamPieceThere(ChessPosition position, ChessGame.TeamColor team, ChessBoard board) {
        // if same color, and at same position return true
        ChessPiece piece = board.getPiece(position);
        if(piece == null) {
            return false;
        }
        return piece.getTeamColor() == team;
    }
}


