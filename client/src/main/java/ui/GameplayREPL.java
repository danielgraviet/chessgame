package ui;

import chess.*;

import client.ServerFacade;
import client.WebSocketCommunicator;
import com.google.gson.Gson;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;

import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Collection;
import java.util.Set;

import static ui.EscapeSequences.*;

public class GameplayREPL implements GameHandlerUI{
    private final ServerFacade serverFacade; // To get authToken
    private final String serverDomain;
    private final int gameID;
    private final ChessGame.TeamColor playerColor; // WHITE, BLACK, or null for observer
    private final String authToken;
    private final PostLoginREPL postLoginREPL; // To return control

    private WebSocketCommunicator wsCommunicator;
    private ChessGame currentGame; // Holds the latest game state received from server
    private final Scanner scanner = new Scanner(System.in);
    private final Gson gson = new Gson();
    private final boolean perspective;

    public GameplayREPL(ServerFacade facade,
                        String domain,
                        int gameID,
                        ChessGame.TeamColor playerColor,
                        String authToken,
                        PostLoginREPL postLoginREPL,
                        ChessGame initialGame) {
        this.serverFacade = facade;
        this.serverDomain = domain;
        this.gameID = gameID;
        this.playerColor = playerColor; // can be null if observing
        this.authToken = authToken;
        this.postLoginREPL = postLoginREPL;
        this.currentGame = initialGame; // use initial game state if provided
        this.perspective = (playerColor != ChessGame.TeamColor.BLACK);
    }

    public void run() {
        try {
            wsCommunicator = new WebSocketCommunicator(serverDomain, this);
            System.out.println("DEBUG: WebSocket connection established.");

            sendConnectCommand();
            System.out.println("DEBUG: Sent CONNECT command.");

            boolean inGame = true;
            while (inGame) {
                String line = scanner.nextLine().trim();
                String[] args = line.split("\\s+");
                String command = args.length > 0 ? args[0].toLowerCase() : "";

                if (command.isEmpty()) {
                    redrawPrompt();
                    continue;
                }


                switch (command) {
                    case "help":
                        printHelp();
                        break;
                    case "redraw":
                        redrawBoard();
                        break;
                    case "leave":
                        sendLeaveCommand();
                        inGame = false; // exit loop
                        break;
                    case "highlight":
                        handleHighlightCommand(args);
                        break;
                    case "move":
                        handleMoveCommand(args);
                        break;
                    case "resign":
                        sendResignCommand();
                        break;
                    default:
                        displayError("Unknown command. Type 'help' for options.");
                        redrawPrompt();
                        break;
                }
            }

        } catch (Exception e) {
            System.out.println(SET_TEXT_COLOR_RED + "FATAL ERROR: Could not start gameplay. " + e.getMessage() + RESET_TEXT_COLOR);
            e.printStackTrace();
        } finally {
            // make sure websocket is closed.
            if (wsCommunicator != null) {
                wsCommunicator.close();
                System.out.println("WebSocket connection closed.");
            }
            System.out.println("Returning to main menu...");
        }
    }

    private void sendConnectCommand() {
        try {
            UserGameCommand connectCmd = new UserGameCommand(UserGameCommand.CommandType.CONNECT, authToken, gameID);
            wsCommunicator.sendMessage(gson.toJson(connectCmd));
        } catch (IOException e) {
            displayError("Failed to send CONNECT command: " + e.getMessage());
        }
    }

    private void sendLeaveCommand() {
        if (wsCommunicator == null) {
            return;
        }
        try {
            UserGameCommand leaveCmd = new UserGameCommand(UserGameCommand.CommandType.LEAVE, authToken, gameID);
            wsCommunicator.sendMessage(gson.toJson(leaveCmd));
        } catch (IOException e) {
            displayError("Failed to send LEAVE command: " + e.getMessage());
            wsCommunicator.close();
        }
    }

    private void sendResignCommand() {
        if (wsCommunicator == null) {
            return;
        }
        if (playerColor == null) {
            displayError("Observers cannot resign.");
            redrawPrompt();
            return;
        }

        try {
            UserGameCommand resignCmd = new UserGameCommand(UserGameCommand.CommandType.RESIGN, authToken, gameID);
            wsCommunicator.sendMessage(gson.toJson(resignCmd));
            // server will notify players about the resignation
        } catch (IOException e) {
            displayError("Failed to send RESIGN command: " + e.getMessage());
        }
    }

    private void handleHighlightCommand(String[] args) {
        if (currentGame == null) {
            displayError("Game state not loaded yet. Cannot highlight moves.");
            return;
        }

        if (args.length != 2) {
            displayError("Invalid command format. Use: highlight <position>");
            displayError("Example: highlight e2");
            return;
        }

        ChessPosition selectedPosition = parsePosition(args[1]);
        if (selectedPosition == null) {
            return;
        }

        // get piece at that pos.
        ChessPiece piece = currentGame.getBoard().getPiece(selectedPosition);
        if (piece == null) {
            displayError("No piece found at " + args[1] + ".");
            return;
        }

        // only correct team can check highlighted moves
        if (playerColor != null && playerColor != piece.getTeamColor()) {
            displayError("You can only highlight your own pieces (" + args[1] + ").");
            return;
        }

        // collection to store moves.
        Collection<ChessMove> validMoves;

        try {
            // getting the moves
            validMoves = currentGame.validMoves(selectedPosition);
        } catch (Exception e) {
            displayError("Error calculating moves for " + args[1] + ": " + e.getMessage());
            return;
        }

        if (validMoves.isEmpty()) {
            displayNotification("Piece at " + args[1] + " has no valid moves.");
            RenderBoard.printBoard(currentGame, this.perspective, null);
            return;
        }

        Set<ChessPosition> highlightPositions = new HashSet<>();
        highlightPositions.add(selectedPosition); // add the selected piece
        for (ChessMove move : validMoves) {
            highlightPositions.add(move.getEndPosition());
        }

        // render the board w highlighted pieces.
        System.out.println("\nHighlighting moves for piece at " + args[1] + ":"); // Info message
        RenderBoard.printBoard(currentGame, this.perspective, highlightPositions);
    }

    private void handleMoveCommand(String[] args) {
        if (wsCommunicator == null) {
            return;
        }

        if (playerColor == null) {
            displayError("Observers cannot make moves.");
            redrawPrompt();
            return;
        }
        if (currentGame == null) {
            displayError("Game state not loaded yet, cannot make moves.");
            redrawPrompt();
            return;
        }
        // is game already over?
        if (currentGame.isGameOver()) {
            displayError("Game is over. No more moves allowed.");
            redrawPrompt();
            return;
        }
        // is it actually my turn?
        if (currentGame.getTeamTurn() != playerColor) {
            displayError("It's not your turn.");
            redrawPrompt();
            return;
        }


        if (args.length < 3) {
            displayError("Invalid move format. Use: move <startPos> <endPos> [promotionPiece]");
            displayError("Example: move e2 e4  OR  move a7 a8 q");
            redrawPrompt();
            return;
        }

        try {
            ChessPosition start = parsePosition(args[1]);
            ChessPosition end = parsePosition(args[2]);
            ChessPiece.PieceType promotion = null;
            if (args.length > 3) {
                promotion = parsePromotionPiece(args[3]);
            }

            if (start == null || end == null) {
                redrawPrompt();
                return;
            }
            if (args.length > 3 && promotion == null) {
                redrawPrompt();
                return;
            }

            ChessMove move = new ChessMove(start, end, promotion);

            ChessPiece pieceAtStart = currentGame.getBoard().getPiece(start);
            if (pieceAtStart == null || pieceAtStart.getTeamColor() != playerColor) {
                displayError("Invalid move: No piece of yours at " + args[1] + ".");
                redrawPrompt();
                return;
            }

            MakeMoveCommand moveCmd = new MakeMoveCommand(authToken, gameID, move);
            wsCommunicator.sendMessage(gson.toJson(moveCmd));

        } catch (IllegalArgumentException e) {
            displayError("Invalid input: " + e.getMessage());
            redrawPrompt();
        } catch (IOException e) {
            displayError("Failed to send MOVE command: " + e.getMessage());
        }
    }

    @Override
    public void displayNotification(String message) {
        System.out.print(ERASE_LINE + "\r"); // Clear current line
        System.out.println(SET_TEXT_COLOR_GREEN + message + RESET_TEXT_COLOR);
    }

    @Override
    public void displayError(String message) {
        System.out.print(ERASE_LINE + "\r"); // Clear current line
        System.out.println(SET_TEXT_COLOR_RED + "ERROR: " + message + RESET_TEXT_COLOR);
    }

    @Override
    public void updateBoard(ChessGame game) {
        this.currentGame = game;
        redrawBoard();
    }

    @Override
    public void redrawPrompt() {
        System.out.print("\n" + "[IN-GAME] >>> ");
    }

    private void redrawBoard() {
        if (currentGame != null) {
            // Use the stored perspective
            RenderBoard.printBoard(currentGame, this.perspective);
        } else {
            System.out.println("No game state available to draw.");
        }
    }

    private void printHelp() {
        System.out.println(SET_TEXT_COLOR_BLUE + """
                Available Commands:
                  help                - Show this message
                  redraw              - Redraw the chessboard
                  leave               - Leave the game (returns to menu)
                  highlight <pos>     - Highlight the possible moves for your piece (e.g, highlight e2)
                  move <start> <end> [promo] - Make a move (e.g., move e2 e4)
                                          Promotion example: move a7 a8 q
                  resign              - Forfeit the game (you stay connected)
                  """
                + RESET_TEXT_COLOR);
        redrawPrompt();
    }

    private ChessPosition parsePosition(String posStr) {
        if (posStr == null || posStr.length() != 2) {
            displayError("Invalid position format: '" + posStr + "'. Use algebraic notation (e.g., 'a1', 'h8').");
            return null;
        }
        char fileChar = Character.toLowerCase(posStr.charAt(0));
        char rankChar = posStr.charAt(1);

        if (fileChar < 'a' || fileChar > 'h' || rankChar < '1' || rankChar > '8') {
            displayError("Invalid position: '" + posStr + "'. File must be a-h, rank must be 1-8.");
            return null;
        }

        int col = fileChar - 'a' + 1; // 'a' -> 1, 'h' -> 8
        int row = Character.getNumericValue(rankChar); // '1' -> 1, '8' -> 8

        return new ChessPosition(row, col);
    }

    private ChessPiece.PieceType parsePromotionPiece(String pieceChar) {
        if (pieceChar == null || pieceChar.length() != 1) {
            displayError("Invalid promotion piece: '" + pieceChar + "'. Use 'q', 'r', 'b', or 'n'.");
            return null;
        }
        return switch (Character.toLowerCase(pieceChar.charAt(0))) {
            case 'q' -> ChessPiece.PieceType.QUEEN;
            case 'r' -> ChessPiece.PieceType.ROOK;
            case 'b' -> ChessPiece.PieceType.BISHOP;
            case 'n' -> ChessPiece.PieceType.KNIGHT;
            default -> {
                displayError("Invalid promotion piece: '" + pieceChar + "'. Must be Q, R, B, or N.");
                yield null;
            }
        };
    }
}
