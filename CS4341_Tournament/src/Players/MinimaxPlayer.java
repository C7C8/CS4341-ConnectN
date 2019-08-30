package Players;

import Referee.Referee;
import Referee.RefereeBoard;
import Utilities.Move;
import Utilities.StateTree;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class MinimaxPlayer extends Player {

	/**
	 * Helper fields for patching a bug in StateTree.
	 */
	private static int MAX_DEPTH = 5;
	private static int LOST = Integer.MIN_VALUE + 1;
	private static int WON = Integer.MAX_VALUE - 1;
	private static int TIE = 0;

	private static PrintStream nullPrintStream = new PrintStream(new OutputStream() {
		@Override
		public void write(int i) throws IOException {
			// Does nothing, because we don't actually want to log anything.
		}
	});

	public MinimaxPlayer(String n, int t, int l) {
		super("MiniMax Player (crmyers)", t, l);
	}

	@Override
	public Move getMove(StateTree state) {
		// One-level maxing implementation because for this specific case we need to return the right move to make
		ArrayList<Move> moves = generateNewMoves(state, turn);

		Move bestMove = moves.get(0);
		int bestValue = Integer.MIN_VALUE;
		for (Move move : moves) {
			final StateTree newState = makeChildState(state, move);
			final int value = minimax(newState, 2, Integer.MIN_VALUE, Integer.MAX_VALUE, turn == 1 ? 2 : 1);
			if (value > bestValue) {
				bestMove = move;
				bestValue = value;
			}
		}

		return bestMove;
	}

	/**
	 * Recursive function for minimax
	 * @param state State of the game at this node
	 * @param depth Current depth
	 * @param alpha Current alpha
	 * @param beta Current beta
	 * @param currentTurn Whose turn is it?
	 * @return The value of this node.
	 */
	public int minimax(StateTree state, final int depth, int alpha, int beta, final int currentTurn) {
		int evaluation = evaluate(state);
		if (depth == MAX_DEPTH || evaluation == WON || evaluation == LOST || evaluation == TIE)
			return evaluation;

		// Do the actual legwork of generating moves, mapping them into child states, and applying minimax to each
		List<StateTree> newStates = generateNewMoves(state, currentTurn).stream()
				.map(move -> makeChildState(state, move))
				.collect(Collectors.toList());

		int best = (turn == currentTurn ? Integer.MIN_VALUE : Integer.MAX_VALUE);
		for (StateTree newState : newStates) {
			int value = minimax(newState, depth + 1, alpha, beta, currentTurn == 1 ? 2 : 1);
			if (turn == currentTurn) {
				// Maximizing
				best = Math.max(best, value);
				alpha = Math.max(alpha, best);
				if (alpha >= beta)
					break; // prune
			}
			else {
				// Minimizing
				best = Math.min(best, value);
				beta = Math.min(beta, best);
				if (alpha >= beta)
					break; // prune
			}
		}
		return best;
	}

	/**
	 * Evaluate the board state *relative to the current player*.
	 * @return Board state evaluation; will be Integer.MAX_VALUE if a win for us or Integer.MIN_VALUE if a loss for us
	 */
	private int evaluate(StateTree state) {
		final int result = Referee.checkForWinner(state);

		// If a player has won and the player that did so is the player we're playing for, return the best possible
		// value. Otherwise return the worst possible value
		if (result == 1 || result == 2)
			return turn == result ? WON : LOST;
		else if (result == 3)
			return TIE;

		Random random = new Random();
		return random.nextInt();
	}

	// ====== HELPERS ======

	/**
	 * Generate a list of new states that could be derived from the given state.
	 * @param startingState Current state
	 * @param player Which player is the one making the move
	 * @return List of states
	 */
	private ArrayList<Move> generateNewMoves(final StateTree startingState, final int player) {
		final ArrayList<Move> moves = new ArrayList<>(startingState.columns);
		final boolean canPop = !(player == 1 ? startingState.pop1 : startingState.pop2);
		for (int i = 0; i < startingState.columns; i++) {
			// Pop move, if applicable
			if (canPop && startingState.getBoardMatrix()[0][i] == turn)
				moves.add(new Move(true, i));

			// Add move, if applicable
			Move move = new Move(false, i);
			if (startingState.getBoardMatrix()[startingState.rows-1][i] == 0)
				moves.add(move);
		}

		return moves;
	}

	/**
	 * Helper for making a child state.
	 * @param state State to base off of
	 * @param move Move to apply
	 * @return New state based on previous, with move applied.
	 */
	private StateTree makeChildState(final StateTree state, Move move) {
		final StateTree newState = new RefereeBoard(
				state.rows,
				state.columns,
				state.winNumber,
				state.turn == 1 ? 2 : 1,
				state.pop1,
				state.pop2,
				state);

		for (int x = 0; x < state.getBoardMatrix().length; x++) {
			for (int y = 0; y < state.getBoardMatrix()[0].length; y++) {
				newState.getBoardMatrix()[x][y] = state.getBoardMatrix()[x][y];
			}
		}

		// Patch the 'out' field to have a null print stream object that just discards any input.
		newState.setOut(nullPrintStream);
		newState.makeMove(move);
		return newState;
	}
}
