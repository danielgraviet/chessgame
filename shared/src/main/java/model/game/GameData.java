package model.game;

import chess.ChessGame;

public record GameData(int gameID, String whiteUser, String blackUser, String gameName, ChessGame game) {
}
