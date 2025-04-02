package ui;

import chess.*;

import client.ServerFacade; // Needed for authToken
import client.WebSocketCommunicator;
import ui.GameHandlerUI;
import com.google.gson.Gson;
import model.game.GameData; // Keep if you pass GameData initially
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

import static ui.EscapeSequences.*;

public class GameplayREPL implements GameHandlerUI{
    // needs these functions
    //    void displayNotification(String message);
    //    void displayError(String message);
    //    void updateBoard(ChessGame game);
    //    void redrawPrompt();

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

    public GameplayREPL(ServerFacade facade, String domain, int gameID, ChessGame.TeamColor playerColor, String authToken, PostLoginREPL postLoginREPL, ChessGame initialGame) {
        this.serverFacade = facade;
        this.serverDomain = domain;
        this.gameID = gameID;
        this.playerColor = playerColor; // Can be null if observing
        this.authToken = authToken;
        this.postLoginREPL = postLoginREPL;
        this.currentGame = initialGame; // Use initial game state if provided
        this.perspective = (playerColor != ChessGame.TeamColor.BLACK);
    }

    public void run() {
        try {
            wsCommunicator = new WebSocketCommunicator(serverDomain, this);
            System.out.println("DEBUG: WebSocket connection established."); // Debug print

            // --- Send CONNECT Command ---
            sendConnectCommand();
            System.out.println("DEBUG: Sent CONNECT command."); // Debug print

            // --- Initial Board Draw (if game state exists) ---
            if (currentGame != null) {
                updateBoard(currentGame); // Show initial state
                redrawPrompt();
            } else {
                System.out.println("Waiting for initial game state from server...");
            }


            // --- Gameplay Loop ---
            boolean inGame = true;
            while (inGame) {
                String line = scanner.nextLine().trim();
                String[] args = line.split("\\s+");
                String command = args.length > 0 ? args[0].toLowerCase() : "";

                // Prevent processing empty input
                if (command.isEmpty()) {
                    redrawPrompt(); // Just show prompt again if user hits enter
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
                        inGame = false; // Exit the loop
                        break;
                    case "move":
                        handleMoveCommand(args);
                        break;
                    case "resign":
                        sendResignCommand();
                        // Note: Resigning doesn't automatically leave. User must type 'leave'.
                        // Server might enforce game over state.
                        break;
                    // Add other commands like 'highlight' if needed later
                    default:
                        displayError("Unknown command. Type 'help' for options.");
                        redrawPrompt();
                        break;
                }
            }

        } catch (Exception e) {
            System.out.println(SET_TEXT_COLOR_RED + "FATAL ERROR: Could not start gameplay. " + e.getMessage() + RESET_TEXT_COLOR);
            e.printStackTrace(); // Print stack trace for debugging
        } finally {
            // --- Ensure WebSocket is closed when loop ends ---
            if (wsCommunicator != null) {
                wsCommunicator.close();
                System.out.println("WebSocket connection closed.");
            }
            System.out.println("Returning to main menu...");
            // Control implicitly returns to PostLoginREPL when run() finishes
        }
    }

    private void sendConnectCommand() {
        try {
            // Observers connect too, playerColor differentiates their role server-side
            UserGameCommand connectCmd = new UserGameCommand(UserGameCommand.CommandType.CONNECT, authToken, gameID);
            // Server needs to associate authToken with playerColor/observer status for this gameID
            wsCommunicator.sendMessage(gson.toJson(connectCmd));
        } catch (IOException e) {
            displayError("Failed to send CONNECT command: " + e.getMessage());
        }
    }

    private void sendLeaveCommand() {
        if (wsCommunicator == null) return;
        try {
            UserGameCommand leaveCmd = new UserGameCommand(UserGameCommand.CommandType.LEAVE, authToken, gameID);
            wsCommunicator.sendMessage(gson.toJson(leaveCmd));
            // The 'finally' block in run() will handle closing the websocket.
        } catch (IOException e) {
            displayError("Failed to send LEAVE command: " + e.getMessage());
            // Still attempt to close locally if send fails
            wsCommunicator.close();
        }
    }

    private void sendResignCommand() {
        if (wsCommunicator == null) return;
        if (playerColor == null) {
            displayError("Observers cannot resign.");
            redrawPrompt();
            return;
        }
        // Add check: Is game already over? (Optional client-side check)
        // if (currentGame != null && currentGame.isGameOver()) {
        //     displayError("Game is already over.");
        //     redrawPrompt();
        //     return;
        // }

        try {
            UserGameCommand resignCmd = new UserGameCommand(UserGameCommand.CommandType.RESIGN, authToken, gameID);
            wsCommunicator.sendMessage(gson.toJson(resignCmd));
            // Server will notify players about the resignation
        } catch (IOException e) {
            displayError("Failed to send RESIGN command: " + e.getMessage());
        }
    }

    private void handleMoveCommand(String[] args) {
        if (wsCommunicator == null) return;
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
        // Add check: Is game already over?
        if (currentGame.isGameOver()) {
            displayError("Game is over. No more moves allowed.");
            redrawPrompt();
            return;
        }
        // Add check: Is it actually my turn?
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
                // Error message handled within parsePosition
                redrawPrompt();
                return;
            }
            if (args.length > 3 && promotion == null) {
                // Error message handled within parsePromotionPiece
                redrawPrompt();
                return;
            }

            ChessMove move = new ChessMove(start, end, promotion);

            // --- Optional: Basic Client-Side Validation ---
            // Check if the piece at startPos belongs to the player
            ChessPiece pieceAtStart = currentGame.getBoard().getPiece(start);
            if (pieceAtStart == null || pieceAtStart.getTeamColor() != playerColor) {
                displayError("Invalid move: No piece of yours at " + args[1] + ".");
                redrawPrompt();
                return;
            }
            // Check if move is pseudo-legal (doesn't consider checks yet fully)
            // Collection<ChessMove> possibleMoves = pieceAtStart.pieceMoves(currentGame.getBoard(), start);
            // if (!possibleMoves.contains(move)) { // Simple contains might not work if promotion needs exact match
            // More robust check needed if doing client-side validation here
            // }


            // --- Send MAKE_MOVE Command ---
            MakeMoveCommand moveCmd = new MakeMoveCommand(authToken, gameID, move);
            wsCommunicator.sendMessage(gson.toJson(moveCmd));
            // Server will validate, execute, and send back LOAD_GAME or ERROR

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
        // Don't redraw prompt here, let the WebsocketCommunicator's handler do it
    }

    @Override
    public void displayError(String message) {
        System.out.print(ERASE_LINE + "\r"); // Clear current line
        System.out.println(SET_TEXT_COLOR_RED + "ERROR: " + message + RESET_TEXT_COLOR);
        // Don't redraw prompt here
    }

    @Override
    public void updateBoard(ChessGame game) {
        this.currentGame = game; // Store the latest game state
        redrawBoard();
        // Don't redraw prompt here
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
                  move <start> <end> [promo] - Make a move (e.g., move e2 e4)
                                          Promotion example: move a7 a8 q
                  resign              - Forfeit the game (you stay connected)
                  """
                // Add highlight commands later if needed
                + RESET_TEXT_COLOR);
        redrawPrompt(); // Show prompt again after help
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

        // Note: ChessPosition internal representation might be (row, col)
        // Ensure this matches your ChessPosition constructor (assuming (row, col))
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
