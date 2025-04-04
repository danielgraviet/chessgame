package ui;
import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;


import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static ui.EscapeSequences.*;

public class RenderBoard {
    public static void printBoard(ChessGame game, boolean whitePerspective, Collection<ChessPosition> highlightedSquares) {
        // error checking
        if (game == null || game.getBoard() == null) {
            System.out.println(SET_TEXT_COLOR_RED + "Error: Cannot print null game or board." + RESET_TEXT_COLOR);
            return;
        }

        // handle null input for highlights
        Set<ChessPosition> highlights = (highlightedSquares == null) ? Collections.emptySet() : new HashSet<>(highlightedSquares);

        ChessBoard board = game.getBoard();
        StringBuilder builder = new StringBuilder();

        // top and bottom column headings
        String columnsWhite = "   a  b  c  d  e  f  g  h   \n";
        String columnsBlack = "   h  g  f  e  d  b  c  a   \n";
        String columns = whitePerspective ? columnsWhite : columnsBlack;

        // top columns
        builder.append(SET_TEXT_COLOR_YELLOW).append(columns).append(RESET_TEXT_COLOR);

        // variables for different perspectives
        int rowStart = whitePerspective ? 8 : 1;
        int rowEnd = whitePerspective ? 0 : 9;
        int rowIncrement = whitePerspective ? -1 : 1;

        for (int visualRow = rowStart; visualRow != rowEnd; visualRow += rowIncrement) {
            int internalRow = visualRow;

            // Print the row label
            builder.append(SET_TEXT_COLOR_YELLOW).append(" ").append(visualRow).append(" ").append(RESET_TEXT_COLOR);

            int colStart = whitePerspective ? 1 : 8;
            int colEnd = whitePerspective ? 9 : 0;
            int colIncrement = whitePerspective ? 1 : -1;

            for (int visualCol = colStart; visualCol != colEnd; visualCol += colIncrement) {
                int internalCol = visualCol;

                ChessPosition currentPosition = new ChessPosition(internalRow, internalCol);
                // way to alternate light squares and dark squares w modulo.
                boolean isLightSquare = (internalRow + internalCol) % 2 != 0;
                boolean shouldHighlight = highlights.contains(currentPosition);
                if (shouldHighlight) {
                    builder.append(SET_BG_COLOR_YELLOW); // set the highlight as yellow. could adjust for alternating colors.
                } else {
                    builder.append(isLightSquare ? SET_BG_COLOR_LIGHT_GREY : SET_BG_COLOR_DARK_GREY);
                }
                // extract the piece at that position
                ChessPiece piece = board.getPiece(currentPosition);

                builder.append(getColoredPieceSymbol(piece));
            }
            builder.append(RESET_BG_COLOR)
                    .append(SET_TEXT_COLOR_YELLOW).append(" ").append(visualRow).append(" ").append(RESET_TEXT_COLOR)
                    .append("\n");
        }
        builder.append(SET_TEXT_COLOR_YELLOW).append(columns).append(RESET_TEXT_COLOR);

        System.out.println(builder);
    }

    // overload
    public static void printBoard(ChessGame game,  boolean whitePerspective) {
        printBoard(game, whitePerspective, null); // calls main method with no highlights.
    }

    private static String getColoredPieceSymbol(ChessPiece piece) {
        if (piece == null) {
            return EMPTY;
        }

        String pieceSymbol;
        String textColor;

        // enhanced switch based on IDE suggestions?
        pieceSymbol = switch (piece.getPieceType()) {
            case PAWN -> WHITE_PAWN;
            case ROOK -> WHITE_ROOK;
            case KNIGHT -> WHITE_KNIGHT;
            case BISHOP -> WHITE_BISHOP;
            case QUEEN -> WHITE_QUEEN;
            case KING -> WHITE_KING;
            default -> "?";
        };
        if (piece.getTeamColor() == ChessGame.TeamColor.BLACK) {
            pieceSymbol = switch (piece.getPieceType()) {
                case PAWN -> BLACK_PAWN;
                case ROOK -> BLACK_ROOK;
                case KNIGHT -> BLACK_KNIGHT;
                case BISHOP -> BLACK_BISHOP;
                case QUEEN -> BLACK_QUEEN;
                case KING -> BLACK_KING;
            };
        }

        textColor = (piece.getTeamColor() == ChessGame.TeamColor.WHITE) ? SET_TEXT_COLOR_WHITE : SET_TEXT_COLOR_BLACK;

        return textColor + pieceSymbol + RESET_TEXT_COLOR;
    }
}
