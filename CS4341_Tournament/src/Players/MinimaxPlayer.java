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
		return minimax(state, 1, Integer.MIN_VALUE, Integer.MAX_VALUE, turn).move;
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
	public MoveTuple minimax(StateTree state, final int depth, int alpha, int beta, final int currentTurn) {
		if (depth == MAX_DEPTH)
			return new MoveTuple(state, null, evaluate(state));

		MoveTuple currentBest = new MoveTuple(null, null, currentTurn == turn ? Integer.MIN_VALUE : Integer.MAX_VALUE);
		List<MoveTuple> newStates = generateNewStates(state, currentTurn == 1 ? 2 : 1);
		for (MoveTuple newState : newStates) {
			MoveTuple temp = minimax(state, depth + 1, alpha, beta, currentTurn == 1 ? 2 : 1);
			newState.value = temp.value;
			if ((turn == currentTurn && newState.value > currentBest.value) // Maximize
					|| (turn != currentTurn && newState.value <currentBest.value)) // Minimize
				currentBest = newState;
		}
		return currentBest;
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
	 * Class for storing moves alongside states
	 */
	private static class MoveTuple {
		Move move;
		StateTree state;
		int value;

		MoveTuple(StateTree state, Move move) {
			this.state = state;
			this.move = move;
		}

		MoveTuple(StateTree state, Move move, int value) {
			this.state = state;
			this.move = move;
			this.value = value;
		}
	}

	/**
	 * Generate a list of new states that could be derived from the given state.
	 * @param startingState Current state
	 * @param player Which player is the one making the move
	 * @return List of states
	 */
	private List<MoveTuple> generateNewStates(final StateTree startingState, final int player) {
		final List<MoveTuple> moveTuples = new ArrayList<>(startingState.columns);
		final boolean canPop = !(player == 1 ? startingState.pop1 : startingState.pop2);
		for (int i = 0; i < startingState.columns; i++) {
			// Non-pop move. If the move wasn't valid, the created object is recycled so we don't have to allocate
			// more memory and copy the object over (expensive!)
			Move move = new Move(false, i);
			StateTree childState = makeChildState(startingState, player);
			boolean wasValid = childState.validMove(move);
			if (wasValid) {
				childState.makeMove(move);
				moveTuples.add(new MoveTuple(childState, move));
			}

			// Pop move, if applicable
			if (canPop) {
				if (wasValid)
					childState = makeChildState(startingState, player);
				move = new Move(true, i);
				if (childState.validMove(move)) {
					childState.makeMove(move);
					moveTuples.add(new MoveTuple(childState, move));
				}
			}
		}

		return moveTuples;
	}

	/**
	 * Helper for making a child state.
	 * @param state
	 * @param turn
	 * @return
	 */
	private StateTree makeChildState(final StateTree state, final int turn) {
		final StateTree newState = new RefereeBoard(
				state.rows,
				state.columns,
				state.winNumber,
				turn,
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
		return newState;
	}
}
