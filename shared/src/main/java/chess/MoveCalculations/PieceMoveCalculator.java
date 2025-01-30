package chess.MoveCalculations;
import chess.ChessBoard;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;

import java.util.Arrays;
import java.util.HashSet;

public interface PieceMoveCalculator {
    static HashSet<ChessMove> getMoves(ChessBoard board, ChessPosition position) {
        return null;
    }

    static boolean isOnBoard(ChessPosition position) {
        int currentRow = position.getRow();
        int currentColumn = position.getColumn();
        return currentRow <= 8 && currentRow >= 1 && currentColumn <= 8 && currentColumn >= 1;
    }

    // for pieces that can move that one space (bishop, queen, rook)
    static HashSet<ChessMove> slidingPieces(ChessBoard board, ChessPosition position, int[][] moveDirections) {
        HashSet<ChessMove> moves = new HashSet<>();
        int currentRow = position.getRow();
        int currentColumn = position.getColumn();

        for (int[] move : moveDirections) {
            int newRow = currentRow;
            int newColumn = currentColumn;

            while (true) {
                newRow += move[0];
                newColumn += move[1];

                ChessPosition newPosition = new ChessPosition(newRow, newColumn);

                if(!PieceMoveCalculator.isOnBoard(newPosition)){
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

    // these are for knight and king pieces, because they can only move a single space at a time
    static HashSet<ChessMove> singleSpacePieces(ChessBoard board, ChessPosition position, int[][] moveDirections) {
        HashSet<ChessMove> moves = new HashSet<>();
        int currentRow = position.getRow();
        int currentColumn = position.getColumn();

        for (int[] move : moveDirections) {
            int newRow = currentRow;
            int newColumn = currentColumn;

            for (int i = 0; i < 1; i++){
                newRow += move[0];
                newColumn += move[1];

                ChessPosition newPosition = new ChessPosition(newRow, newColumn);

                if(!PieceMoveCalculator.isOnBoard(newPosition)){
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
