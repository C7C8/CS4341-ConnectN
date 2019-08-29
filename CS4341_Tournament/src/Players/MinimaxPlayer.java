package Players;

import Referee.RefereeBoard;
import Utilities.Move;
import Utilities.StateTree;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class MinimaxPlayer extends Player {

	/**
	 * Helper fields for patching a bug in StateTree.
	 */
	private static Field stateTreeOutField = null;
	private static PrintStream nullPrintStream = null;

	static {
		try {
			stateTreeOutField = StateTree.class.getDeclaredField("out");
			stateTreeOutField.setAccessible(true);
			nullPrintStream = new PrintStream(new OutputStream() {
				@Override
				public void write(int i) throws IOException {
					// Does nothing, because we don't actually want to log anything.
				}
			});
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
	}


	public MinimaxPlayer(String n, int t, int l) {
		super("MiniMax Player (crmyers)", t, l);
	}

	@Override
	public Move getMove(StateTree state) {
		int currentTurn = turn;
		int beta 


		return null;
	}

	/**
	 * Evaluate the board state *relative to the current player*.
	 * @return Board state evaluation; will be Integer.MAX_VALUE if a win for us or Integer.MIN_VALUE if a loss for us
	 */
	private int evaluate(StateTree state) {
		return 0;
	}

	/**
	 * Generate a list of new states that could be derived from the given state.
	 * @param startingState Current state
	 * @param player Which player is the one making the move
	 * @return List of states
	 */
	private List<StateTree> generateNewStates(final StateTree startingState, final int player) {
		final List<StateTree> states = new ArrayList<>(startingState.columns);
		final boolean canPop = !(player == 1 ? startingState.pop1 : startingState.pop2);
		for (int i = 0; i < startingState.columns; i++) {
			// Non-pop move. If the move wasn't valid, the created object is recycled so we don't have to allocate
			// more memory and copy the object over (expensive!)
			Move move = new Move(false, i);
			StateTree childState = makeChildState(startingState, player);
			boolean wasValid = childState.validMove(move);
			if (wasValid) {
				childState.makeMove(move);
				states.add(childState);
			}

			// Pop move, if applicable
			if (canPop) {
				if (wasValid)
					childState = makeChildState(startingState, player);
				move = new Move(true, i);
				if (childState.validMove(move)) {
					childState.makeMove(move);
					states.add(childState);
				}
			}
		}

		return states;
	}

	private StateTree makeChildState(final StateTree state, final int turn) {
		final RefereeBoard newState = new RefereeBoard(
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
		try {
			stateTreeOutField.set(newState, nullPrintStream);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		return newState;
	}
}
