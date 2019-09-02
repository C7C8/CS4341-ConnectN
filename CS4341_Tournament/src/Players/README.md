# Minimax Player Implementation
*Author: Christopher Myers (crmyers@wpi.edu)*

## About

This is a simple implementation of the Minimax algorithm with a fairly simple heuristic. The heuristic works by tallying
up all the lines of pieces in a row that a player has, then adding them together with appropriate weights. The final
score is based on the square of a path-length count times the cube of the path's length. This gives increasing advantages
to players who increase the number of paths they have on the board (i.e. more possible victories) and gives even heavier
advantages to those with longer paths. As a result, the minimax player can play strongly defensively to prevent the
opponent from advancing, while also taking advantage of opportunities to advance its own goals.

The heuristic does ***not*** contain a win/loss/tie detection factor because the minimax algorithm runs that before it
attempts to run the evaluation function. States with a win/loss/tie are considered leaf nodes (as they should be).

This heuristic function is definitely on the simple side, but having a simple heuristic means the player has more time
to explore the game tree, which I've capped at 9 ply.

### Limitations

Diagonal paths aren't considered by the heuristic evaluation, so the minimax player won't see them heavily weighted.
***However***, win/loss detection *does* see them (I just reused the board check function) and report wins or losses
as positive or negative infinity, respectively. As a result the minimax player will still play defensively against diagonal
wins, and will still attempt to use its own accidentally-created diagonal paths to win.

The heuristic also does not consider N-in-a-row with a gap somewhere in between, e.g. X-XX, so it won't attempt any kind
of gap-filling strategy. I decided against correcting for this deficiency in the name of performance, as omitting it lets
the player move faster and explore deeper.

## Grading

**This player is self-contained!** I did all my development work inside the CS4341_Tournament project, the only thing it
assumes it that `StateTree.pop1` and `StateTree.pop2` are public, but that modification was stated to be allowed.

## Experimentation

To determine an optimal evaluation function I experimented with a number of heuristics, with the baseline always being
how often minimax was able to win vs. SimplePlayer. I treated *any* win by SimplePlayer as a failure of the heuristic I
built, as SimplePlayer has no intelligence and should be readily beatable by anything smarter than a toddler. In the
early stages of development, this was a *really* good way to prune out bad designs. I also considered time-to-move,
with the goal of having the player be able to move in under a second or two.

My first design -- just to test minimax alone -- was simple win/loss/tie, with wins being positive infinity, losses being
negative infinity, ties set to 0, and all other results set to a random number. Hilariously this worked and met my first
criterion (it could prevent SimplePlayer from winning), but it wasn't very efficient at winning. I decided that the AI
should always try to win if it can do so without opening itself up to loss.

My second design used a cluster-detection algorithm to reward players that had more "clusters" on the board -- pieces of
their own that are all connected together -- based on a simple depth-first search. That was moderately effective, but
it led to minimax creating snakes of its own tiles throughout the board instead of going for a win, and it wasn't quite
playing defensively enough. It could still lose to SimplePlayer in a few cases, so it was unacceptable.

My third design expanded on that concept by applying weights to the clustering scored based on what a player's longest
path in the board was. Bizarrely, that didn't work at all, and led to SimplePlayer defeating Minimax regularly. I'm still
not exactly sure what went wrong.

My fourth and final design removed all of the cluster detection algorithm in favor of a modification to path detection,
wherein the sum of all your paths was considered, with longer paths being weighted more heavily. I experimented with a
few constants (mostly the exponents in weighting an a constant value added on to path length and count) to find the
right balance between defensive play and aggressive play. I also tweaked the move generation function to shuffle the
moves it generates before returning them to minimax, which completely removed the bias towards playing on the left side
of the board.