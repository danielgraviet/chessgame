import chess.*;
import client.*;
import ui.renderBoard;

public class Main {
    public static void main(String[] args) {
        var piece = new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN);
        System.out.println("♕ 240 Chess Client: " + piece);
        ChessGame game = new ChessGame();
        ui.renderBoard.printBoard(game, false);
    }
}