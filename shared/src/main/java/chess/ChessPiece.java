package chess;

import java.util.Collection;
import java.util.Objects;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {
    private final ChessGame.TeamColor teamColor;
    private final PieceType pieceType;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.teamColor = pieceColor;
        this.pieceType = type;
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return teamColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return pieceType;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        if (pieceType == PieceType.BISHOP) {
            return chess.movecalculations.CalculateBishopMoves.getMoves(board, myPosition);
        } else if (pieceType == PieceType.KING){
            return chess.movecalculations.CalculateKingMoves.getMoves(board, myPosition);
        } else if (pieceType == PieceType.QUEEN) {
            return chess.movecalculations.CalculateQueenMoves.getMoves(board, myPosition);
        } else if (pieceType == PieceType.ROOK) {
            return chess.movecalculations.CalculateRookMoves.getMoves(board, myPosition);
        } else if (pieceType == PieceType.KNIGHT) {
            return chess.movecalculations.CalculateKnightMoves.getMoves(board, myPosition);
        } else if (pieceType == PieceType.PAWN) {
            return chess.movecalculations.CalculatePawnMoves.getMoves(board, myPosition);
        } else {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessPiece piece = (ChessPiece) o;
        return teamColor == piece.teamColor && pieceType == piece.pieceType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamColor, pieceType);
    }

    @Override
    public String toString() {
        return teamColor + " " + pieceType;
    }
}
