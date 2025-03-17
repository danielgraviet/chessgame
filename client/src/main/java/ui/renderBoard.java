package ui;
import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;

import java.util.Objects;

import static ui.EscapeSequences.*;

public class renderBoard {
    public static void printBoard(ChessGame game, boolean whitePerspective) {
        ChessBoard board = game.getBoard();
        StringBuilder builder = new StringBuilder();
        builder.append(ERASE_SCREEN);

        // top and bottom column headings
        String columns = "   a  b  c  d  e  f  g  h   \n";

        // variables for different perspectives
        int startRow = whitePerspective ? 1 : 8;
        int endRow = whitePerspective ? 9 : 0;
        int increment = whitePerspective ? 1 : -1;

        // append top row that display's columns. think of stacking a sandwich
        builder.append(SET_TEXT_COLOR_YELLOW)
                .append(columns)
                .append(RESET_TEXT_COLOR);

        for (int row = startRow; row != endRow; row += increment) {

            // add row numbers
            builder.append(SET_TEXT_COLOR_YELLOW)
                    .append(row)
                    .append(" ")
                    .append(RESET_TEXT_COLOR);

            for (int col = 1; col <= 8; col++) {
                // way to alternate light squares and dark squares w modulo.
                boolean isLightSquare = (row + col) % 2 == 0;
                builder.append(isLightSquare ? SET_BG_COLOR_WHITE : SET_BG_COLOR_BLACK);

                // extract the piece at that position
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);

                if (piece == null) {
                    builder.append(EMPTY);
                } else {
                    builder.append(convertPieceToSymbol(piece));
                }
            }
            builder.append(RESET_BG_COLOR)
                    .append(SET_TEXT_COLOR_YELLOW)
                    .append(" ")
                    .append(row)
                    .append(RESET_TEXT_COLOR)
                    .append("\n");
        }

        // append the bottom row
        builder.append(SET_TEXT_COLOR_YELLOW)
                .append(columns)
                .append(RESET_TEXT_COLOR);

        System.out.println(builder);
    }

    public static String convertPieceToSymbol(ChessPiece piece) {
        if (piece == null) {
            return EMPTY;
        } else if (Objects.equals(piece.toString(), "WHITE PAWN")) {
            return WHITE_PAWN;
        } else if (Objects.equals(piece.toString(), "WHITE KING")) {
            return WHITE_KING;
        } else if (Objects.equals(piece.toString(), "WHITE QUEEN")) {
            return WHITE_QUEEN;
        } else if (Objects.equals(piece.toString(), "WHITE KNIGHT")) {
            return WHITE_KNIGHT;
        } else if (Objects.equals(piece.toString(), "WHITE BISHOP")) {
            return WHITE_BISHOP;
        } else if (Objects.equals(piece.toString(), "WHITE ROOK")) {
            return WHITE_ROOK;


        } else if (Objects.equals(piece.toString(), "BLACK PAWN")) {
            return BLACK_PAWN;
        } else if (Objects.equals(piece.toString(), "BLACK KING")) {
            return BLACK_KING;
        } else if (Objects.equals(piece.toString(), "BLACK QUEEN")) {
            return BLACK_QUEEN;
        } else if (Objects.equals(piece.toString(), "BLACK KNIGHT")) {
            return BLACK_KNIGHT;
        } else if (Objects.equals(piece.toString(), "BLACK BISHOP")) {
            return BLACK_BISHOP;
        } else if (Objects.equals(piece.toString(), "BLACK ROOK")) {
            return BLACK_ROOK;
        } else {
            return "INVALID PIECE";
        }
    }
}
