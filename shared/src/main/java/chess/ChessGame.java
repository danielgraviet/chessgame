package chess;

import java.util.*;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {

    private ChessBoard board;
    private TeamColor teamTurn;

    public ChessGame() {
        teamTurn = TeamColor.WHITE;
        board = new ChessBoard();
        board.resetBoard();
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return teamTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        teamTurn = team;
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }
    /**
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece currentPiece = board.getPiece(startPosition);

        if (currentPiece == null) {
            return new HashSet<>();
        }
        // get all moves for the piece at the position
        HashSet<ChessMove> possibleMoves = (HashSet<ChessMove>) currentPiece.pieceMoves(board, startPosition);

        // hash set to store all the valid moves.
        HashSet<ChessMove> validMoves = new HashSet<>();

        // loop through all possible moves
        for (ChessMove move : possibleMoves) {
            ChessPiece temporaryPiece = board.getPiece(move.getEndPosition());
            // this places the piece at the new position to simulate a move.
            board.addPiece(move.getEndPosition(), currentPiece);

            // this removes the current piece from the old position on the board, to simulate a move
            board.addPiece(startPosition, null);

            // now that we have fully simulated a move, we need to check if our king is in check
            if (!isInCheck(currentPiece.getTeamColor())) { // param is our team color.
                validMoves.add(move);
            }

            // now we need to reset the board and do the next move.
            board.addPiece(move.getEndPosition(), temporaryPiece);
            board.addPiece(startPosition, currentPiece);
        }
        return validMoves;
    }
    /**
     * Makes a move in a chess game
     *
     * @param move chess move to preform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {

        ChessPosition piecePosition = move.getStartPosition();

        // checks if the starting position has piece
         if (board.getPiece(piecePosition) == null) {
             throw new InvalidMoveException();
         }

        boolean teamTurn = getTeamTurn() == board.getPiece(piecePosition).getTeamColor();
        Collection<ChessMove> validMoves = validMoves(piecePosition);

        if (validMoves.isEmpty()) {
            throw new InvalidMoveException("There are no more moves to make");
        }

        boolean isValidMove = validMoves.contains(move);

        if (isValidMove && teamTurn){
            ChessPiece pieceToMove = board.getPiece(piecePosition);
            if (move.getPromotionPiece() != null){
                pieceToMove = new ChessPiece(pieceToMove.getTeamColor(), move.getPromotionPiece());
            }

            // move the piece
            board.addPiece(move.getEndPosition(), pieceToMove);
            // remove the old piece
            board.addPiece(move.getStartPosition(), null);
            setTeamTurn(getTeamTurn() == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE);
        } else {
            throw new InvalidMoveException("There are no more moves to make");
        }
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition kingPosition = null;
        // compare enemy moves to king position at the end
        List<ChessMove> enemyMoves = new ArrayList<>();

        // first find the king and enemy pieces
        for (int row = 1; row <= 8; row++) {
            for (int column = 1; column <= 8; column++) {
                ChessPiece pieceAtNewPosition = board.getPiece(new ChessPosition(row, column));
                // piece has been encountered
                if (pieceAtNewPosition != null) {
                    // found king
                    if (pieceAtNewPosition.getTeamColor() == teamColor && ChessPiece.PieceType.KING == pieceAtNewPosition.getPieceType()) {
                        kingPosition = new ChessPosition(row, column);

                    // enemy piece has been encountered
                    } else if (pieceAtNewPosition.getTeamColor() != teamColor) {
                        ChessPiece enemyPiece = board.getPiece(new ChessPosition(row, column));
                        ChessPosition enemyPosition = new ChessPosition(row, column);
                        // add its moves.
                        enemyMoves.addAll(enemyPiece.pieceMoves(board, enemyPosition));
                    }
                }
            }
        }
        for (ChessMove move : enemyMoves) {
            if (move.getEndPosition().equals(kingPosition)) {
                return true;
            }
        }

        return false;
    }

    private boolean noValidMoves(TeamColor teamColor) {
        // iterates through board
        for (int row = 1; row <= 8; row++) {
            for (int column = 1; column <= 8; column++) {
                // finds pieces and sees if they have valid moves.
                ChessPiece pieceAtNewPosition = board.getPiece(new ChessPosition(row, column));
                ChessPosition position = new ChessPosition(row, column);

                if (pieceAtNewPosition != null && pieceAtNewPosition.getTeamColor() == teamColor) {
                    if (!validMoves(position).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        if (!isInCheck(teamColor)) {
            return false;
        }
        // if it is IN CHECK && there are no valid moves, it is a checkmate. `
        return noValidMoves(teamColor);
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        if (isInCheck(teamColor)) {
            return false;
        }
        // if there are no valid moves && it is not in check, is a stalemate.
        return noValidMoves(teamColor);
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = board;
    }
    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return board;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessGame chessGame = (ChessGame) o;
        return (board.equals(chessGame.board)) && teamTurn == chessGame.teamTurn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(board, teamTurn);
    }

    @Override
    public String toString() {
        return board.toString();
    }
}
