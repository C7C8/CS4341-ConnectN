package Players;

import Referee.Referee;
import Referee.RefereeBoard;
import Utilities.Move;
import Utilities.StateTree;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class MinimaxPlayer extends Player {

	/**
	 * Helper fields for patching a bug in StateTree.
	 */
	private static int MAX_DEPTH = 7;
	private static int LOST = Integer.MIN_VALUE + 1;
	private static int WON = (Integer.MAX_VALUE - 1) / 2;
	private static int TIE = 0;

	private static PrintStream nullPrintStream = new PrintStream(new OutputStream() {
		@Override
		public void write(int i) throws IOException {
			// Does nothing, because we don't actually want to log anything.
		}
	});

	public MinimaxPlayer(String n, int t, int l) {
		super(n, t, l);
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
		int evaluation = evaluate(state, depth);
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
	private int evaluate(StateTree state, int depth) {
		final int result = Referee.checkForWinner(state);

		// Basics: detect leaf nodes, weight accordingly. Ties are counted as neutral.
		if (result == 1 || result == 2)
			return turn == result ? WON : LOST;
		else if (result == 3)
			return TIE;

		// Cluster detection -- find clusters for both players, sum and weight accordingly
		// Maximum cluster size is rows*columns since you can't fit anything more on the board

		// NOTE ABOUT THE BIT HACKING: This DFS implementation uses some bit twiddling to determine if a square on the
		// board has been explored yet or not. This is because the alternative was an rows*columns array of booleans
		// that had to be re-allocated for every search, which is crazy expensive. I shaved some time off in the inner
		// loop by setting a flag in the board matrix, which requires no additional memory and saves an ownership check
		// later. This might seem minor, but the optimization yielded an overall 100%+ increase in the whole minimax
		// implementation performance!
		int[][] clusters = new int[2][state.rows * state.columns + 1];
		Stack<Point> stack = new Stack<>();
		for (int x = 0; x < state.rows; x++) {
			for (int y = 0; y < state.rows; y++) {
				// The 0x10'th bit is used to determine whether that tile has been explored or not.
				if ((state.getBoardMatrix()[x][y] & 0x10) > 0)
					continue;

				// Found an unexplored tile! Do DFS to find the cluster size.
				final int player = state.turn;
				int count = 0;
				stack.push(new Point(x, y));
				while (!stack.empty()) {
					// Pop node off stack, mark it as explored, and increase the count
					final Point p = stack.pop();
					state.getBoardMatrix()[p.x][p.y] |= 0x10;
					count++;

					// Explore neighbors
					for (int ix = -1; ix <= 1; ix++) {
						for (int iy = -1; iy <= 1; iy++) {
							// Don't explore self
							if (ix == 0 && iy == 0)
								continue;

							// Don't explore outside the board
							final int nx = p.x + ix;
							final int ny = p.y + iy;
							if (nx < 0 || nx >= state.rows || ny < 0 || ny >= state.columns)
								continue;

							// Don't explore previously-explored areas or areas that don't belong to this player.
							// Because the player owning the square will have the 0x10'th bit set, this ownership check
							// also doubles as an exploration check.
							if (state.getBoardMatrix()[nx][ny] != player)
								continue;

							// Add this node to the stack!
							stack.push(new Point(nx, ny));
						}
					}
				}

				// Clean up by unsetting all the 0x10'th bits across the board
				for (int i = 0; i < state.rows; i++) {
					for (int j = 0; j < state.columns; j++) {
						state.getBoardMatrix()[i][j] &= ~0x10;
					}
				}

				// Node fully explored; add the discovered cluster count to the clusters array
				clusters[player - 1][count] += 1;
			}
		}

		// Board fully explored; calculate weights by multiplying the square of the cluster count by the cube of the
		// cluster size. Opposing player's scores are negative.
		int score = 0;
		for (int clusterSize = 1; clusterSize < state.rows * state.columns; clusterSize++) {
			// i'm lazy
			for (int player = 0; player < 2; player++) {
				final int clusterCount = clusters[player][clusterSize];
				final int weightedCount = (clusterCount * clusterCount) * (clusterSize * clusterSize * clusterSize);
				score += ((player + 1) == turn ? weightedCount : -weightedCount);
			}
		}

		return score;
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
				state.turn,
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
