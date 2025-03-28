package chess.movecalculations;

import chess.ChessBoard;
import chess.ChessPosition;
import chess.*;

import java.util.Collection;
import java.util.HashSet;

public class CalculatePawnMoves implements PieceMoveCalculator {
    public static Collection<ChessMove> getMoves(ChessBoard board, ChessPosition position) {
        HashSet<ChessMove> moves = new HashSet<>();
        int currentRow = position.getRow();
        ChessPiece currentPiece = board.getPiece(position);

        // check early for invalid piece
        if (currentPiece == null) {
            return moves;
        }

        ChessGame.TeamColor teamColor = currentPiece.getTeamColor();

        int direction = (teamColor == ChessGame.TeamColor.WHITE) ? 1 : -1;
        int startingRow = (teamColor == ChessGame.TeamColor.WHITE ? 2 : 7);

        if (currentRow == startingRow) {
            moves.addAll(CalculatePawnMoves.startingMoves(board, position, direction));
        } else {
            moves.addAll(CalculatePawnMoves.regularMoves(board, position, direction));
        }
        moves.addAll(CalculatePawnMoves.attackMoves(board, position, direction));
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
            }
            ChessPiece newPiece = board.getPiece(newPosition);

            if (newPiece != null) {
                break;
            } else {
                startingMoves.add(new ChessMove(position, newPosition, null));
            }

        }
        return startingMoves;
    }

    public static Collection<ChessMove> attackMoves(ChessBoard board, ChessPosition position, int direction) {
        HashSet<ChessMove> moves = new HashSet<>();
        int currentRow = position.getRow();
        int currentColumn = position.getColumn();
        ChessPiece currentPiece = board.getPiece(position);
        int[][] attackMoves = {{direction,1}, {direction,-1}};
        ChessGame.TeamColor teamColor = currentPiece.getTeamColor();

        for (int[] move: attackMoves) {
            int newRow = currentRow + move[0];
            int newColumn = currentColumn + move[1];
            ChessPosition newPosition = new ChessPosition(newRow, newColumn);

            if (!PieceMoveCalculator.isOnBoard(newPosition)){
                continue;
            }
            ChessPiece newPiece = board.getPiece(newPosition);

            if (newPiece != null && newPiece.getTeamColor() != currentPiece.getTeamColor()) {
                CalculatePawnMoves.addMoveOrPromotion(moves, position, newPosition, teamColor);
            }
        }
        return moves;
    }

    public static Collection<ChessMove> regularMoves(ChessBoard board, ChessPosition position, int direction) {
        HashSet<ChessMove> moves = new HashSet<>();
        int currentRow = position.getRow();
        int currentColumn = position.getColumn();
        ChessPiece currentPiece = board.getPiece(position);
        ChessGame.TeamColor teamColor = currentPiece.getTeamColor();

        // move forward one spot
        int newRow = currentRow + direction;
        ChessPiece newPiece = board.getPiece(new ChessPosition(newRow, currentColumn));
        ChessPosition newPosition = new ChessPosition(newRow, currentColumn);

        if (!PieceMoveCalculator.isOnBoard(newPosition)){
            return moves;
        }

        // check if ANY piece is there
        if (newPiece != null) {
            return moves;
        } else {
            CalculatePawnMoves.addMoveOrPromotion(moves, position, newPosition, teamColor);
        }
        return moves;
    }

    public static void addMoveOrPromotion(
            HashSet<ChessMove> moves,  // takes existing move hashset and sees if it needs promotion, or moves.
            ChessPosition position,
            ChessPosition newPosition,
            ChessGame.TeamColor teamColor)
    {

        if (CalculatePawnMoves.needsPromotion(teamColor, newPosition.getRow())) {
            CalculatePawnMoves.addPromotionPieces(moves, position, newPosition);
        } else {
            moves.add(new ChessMove(position, newPosition, null));
        }
    }

    public static boolean needsPromotion(ChessGame.TeamColor teamColor, int row) {
        if (teamColor == ChessGame.TeamColor.WHITE  && row == 8) {
            return true;
        } else {
            return teamColor == ChessGame.TeamColor.BLACK && row == 1;
        }
    }

    public static void addPromotionPieces(HashSet<ChessMove> moves, ChessPosition position, ChessPosition newPosition) {
        for (ChessPiece.PieceType promotionPiece : ChessPiece.PieceType.values()) {
            if (promotionPiece != ChessPiece.PieceType.PAWN && promotionPiece != ChessPiece.PieceType.KING) {
                moves.add(new ChessMove(position, newPosition, promotionPiece));
            }
        }
    }
}

