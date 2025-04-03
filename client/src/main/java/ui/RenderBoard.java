package ui;
import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;


import static ui.EscapeSequences.*;

public class RenderBoard {
    public static void printBoard(ChessGame game, boolean whitePerspective) {
        // error checking
        if (game == null || game.getBoard() == null) {
            System.out.println(SET_TEXT_COLOR_RED + "Error: Cannot print null game or board." + RESET_TEXT_COLOR);
            return;
        }

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
                // way to alternate light squares and dark squares w modulo.
                boolean isLightSquare = (internalRow + internalCol) % 2 != 0;
                builder.append(isLightSquare ? SET_BG_COLOR_LIGHT_GREY : SET_BG_COLOR_DARK_GREY);

                // extract the piece at that position
                ChessPosition position = new ChessPosition(internalRow, internalCol);
                ChessPiece piece = board.getPiece(position);

                builder.append(getColoredPieceSymbol(piece));
            }
            builder.append(RESET_BG_COLOR)
                    .append(SET_TEXT_COLOR_YELLOW).append(" ").append(visualRow).append(" ").append(RESET_TEXT_COLOR)
                    .append("\n");
        }
        builder.append(SET_TEXT_COLOR_YELLOW).append(columns).append(RESET_TEXT_COLOR);

        System.out.println(builder);
    }

    private static String getColoredPieceSymbol(ChessPiece piece) {
        if (piece == null) {
            return EMPTY;
        }

        String pieceSymbol;
        String textColor;

        switch (piece.getPieceType()) {
            case PAWN:   pieceSymbol = WHITE_PAWN;   break;
            case ROOK:   pieceSymbol = WHITE_ROOK;   break;
            case KNIGHT: pieceSymbol = WHITE_KNIGHT; break;
            case BISHOP: pieceSymbol = WHITE_BISHOP; break;
            case QUEEN:  pieceSymbol = WHITE_QUEEN;  break;
            case KING:   pieceSymbol = WHITE_KING;   break;
            default:     pieceSymbol = "?";          break;
        }
        if (piece.getTeamColor() == ChessGame.TeamColor.BLACK) {
            switch (piece.getPieceType()) {
                case PAWN:   pieceSymbol = BLACK_PAWN;   break;
                case ROOK:   pieceSymbol = BLACK_ROOK;   break;
                case KNIGHT: pieceSymbol = BLACK_KNIGHT; break;
                case BISHOP: pieceSymbol = BLACK_BISHOP; break;
                case QUEEN:  pieceSymbol = BLACK_QUEEN;  break;
                case KING:   pieceSymbol = BLACK_KING;   break;
            }
        }

        textColor = (piece.getTeamColor() == ChessGame.TeamColor.WHITE) ? SET_TEXT_COLOR_WHITE : SET_TEXT_COLOR_BLACK;

        return textColor + pieceSymbol + RESET_TEXT_COLOR;
    }
}
