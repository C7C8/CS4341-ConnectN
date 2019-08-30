package Players;

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
import java.util.stream.Stream;

public class MinimaxPlayer extends Player {

	/**
	 * Helper fields for patching a bug in StateTree.
	 */
	private static int MAX_DEPTH = 3;
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
		List<Move> moves = generateNewMoves(state, turn);
		List<StateTree> states = moves.stream().map(move -> makeChildState(state, move)).collect(Collectors.toList());
		Move bestMove = null;
		int bestValue = Integer.MIN_VALUE;
		for (int i = 0; i < states.size(); i++) {
			final int value = minimax(states.get(i), 2, Integer.MIN_VALUE, Integer.MAX_VALUE, turn == 1 ? 2 : 1);
			if (value > bestValue) {
				bestMove = moves.get(i);
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
		if (depth == MAX_DEPTH)
			return evaluate(state);

		// Do the actual legwork of generating moves, mapping them into child states, and applying minimax to each
		Stream<Integer> results = generateNewMoves(state, currentTurn).stream()
				.map(move -> makeChildState(state, move))
				.map(newState -> minimax(newState, depth + 1, alpha, beta, currentTurn == 1 ? 2 : 1));

		// Return the maximum or minimum depending on whether this turn is for the current player or the opponent
		if (turn == currentTurn)
			return results.max(Integer::compareTo).orElse(Integer.MIN_VALUE);
		else
			return results.min(Integer::compareTo).orElse(Integer.MAX_VALUE);
	}

	/**
	 * Evaluate the board state *relative to the current player*.
	 * @return Board state evaluation; will be Integer.MAX_VALUE if a win for us or Integer.MIN_VALUE if a loss for us
	 */
	private int evaluate(StateTree state) {
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
	private List<Move> generateNewMoves(final StateTree startingState, final int player) {
		final List<Move> moves = new ArrayList<>(startingState.columns);
		final boolean canPop = !(player == 1 ? startingState.pop1 : startingState.pop2);
		for (int i = 0; i < startingState.columns; i++) {
			Move move = new Move(false, i);
			if (startingState.validMove(move))
				moves.add(move);

			// Pop move, if applicable
			if (canPop) {
				move = new Move(true, i);
				if (startingState.validMove(move))
					moves.add(move);
			}
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
