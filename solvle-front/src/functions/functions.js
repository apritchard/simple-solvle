export const ALLOWABLE_CHARACTERS = "AÁÄBCDÐEÉFGHIÍJKLMNÑOÓPQRSẞTUÚÜVWXYÝZÞÆÖ"

// Per-tile color states. These double as the CSS class names applied to a tile.
// The click cycle runs default -> gray -> yellow -> green -> default.
export const TILE_DEFAULT = "";       // entered but uncolored
export const TILE_GRAY = "error";     // absent, or a surplus duplicate
export const TILE_YELLOW = "almost";  // present, wrong position
export const TILE_GREEN = "correct";  // present, correct position

// A tile whose color the user has not set. Such tiles are auto-colored from the
// board-wide facts (propagation); set tiles are the only source of evidence.
export const TILE_UNSET = null;

const TILE_CYCLE = [TILE_DEFAULT, TILE_GRAY, TILE_YELLOW, TILE_GREEN];

export function nextTileColor(color) {
    const i = TILE_CYCLE.indexOf(color);
    return TILE_CYCLE[(i + 1) % TILE_CYCLE.length];
}

/**
 * Creates an empty tileColors grid (rows x width) of TILE_UNSET values.
 */
export function initialTileColors(rows, width) {
    const grid = [];
    for (let r = 0; r < rows; r++) {
        grid[r] = [];
        for (let c = 0; c < width; c++) {
            grid[r][c] = TILE_UNSET;
        }
    }
    return grid;
}

/**
 * Computes the true Wordle feedback colors for a guess against a known solution
 * using the standard two-pass algorithm. Returns an array of TILE_* values.
 */
export function computeFeedback(guess, solution) {
    const g = (guess || "").toUpperCase().split("");
    const s = (solution || "").toUpperCase().split("");
    const result = g.map(() => TILE_GRAY);

    const remaining = {};
    s.forEach(ch => { remaining[ch] = (remaining[ch] || 0) + 1; });

    for (let i = 0; i < g.length; i++) {
        if (g[i] !== "" && g[i] === s[i]) {
            result[i] = TILE_GREEN;
            remaining[g[i]]--;
        }
    }
    for (let i = 0; i < g.length; i++) {
        if (result[i] === TILE_GREEN || g[i] === "") continue;
        if (remaining[g[i]] > 0) {
            result[i] = TILE_YELLOW;
            remaining[g[i]]--;
        }
    }
    return result;
}

/**
 * Derives board-wide knowledge from the tiles the user has explicitly set
 * (TILE_UNSET tiles are propagation-only and never count as evidence).
 *
 * Returns:
 *  - known:    Map<position, letter>      green positions
 *  - excluded: Map<position, Set<letter>> letters known not at that position
 *  - minCount: Map<letter, number>        copies proven present
 *  - maxCount: Map<letter, number|null>   upper bound; 0 = absent, null = unbounded
 *
 * minCount uses the most copies seen in any single guess (green+yellow) and the
 * number of distinct green positions. maxCount comes from a guess where the letter
 * was played more than it was present (a set gray alongside fewer present copies);
 * a cap that contradicts the minimum is dropped so explicit "present" wins.
 */
export function deriveGlobalState(board, colors) {
    const known = new Map();
    const excluded = new Map();
    const greenPositions = new Map(); // letter -> Set(pos)
    const yellowsByRow = [];          // per row: Map letter -> set-yellow count
    const grayCountByRow = [];            // per row: Set(letter) with a set gray

    const addExcluded = (p, letter) => {
        if (!excluded.has(p)) excluded.set(p, new Set());
        excluded.get(p).add(letter);
    };

    // Pass 1: green positions and exclusions (so `known` is complete before counting).
    for (let r = 0; r < board.length; r++) {
        const row = board[r] || [];
        const rc = (colors && colors[r]) || [];
        const yellows = new Map();
        const grayCount = new Map();

        for (let p = 0; p < row.length; p++) {
            const letter = row[p];
            if (!letter) continue;
            const color = rc[p];
            if (color === TILE_GREEN) {
                known.set(p, letter);
                if (!greenPositions.has(letter)) greenPositions.set(letter, new Set());
                greenPositions.get(letter).add(p);
            } else if (color === TILE_YELLOW) {
                addExcluded(p, letter);
                yellows.set(letter, (yellows.get(letter) || 0) + 1);
            } else if (color === TILE_GRAY) {
                addExcluded(p, letter);
                grayCount.set(letter, (grayCount.get(letter) || 0) + 1);
            }
        }
        yellowsByRow.push(yellows);
        grayCountByRow.push(grayCount);
    }

    // Pass 2: present copies per row = displayed greens (set green, or an auto-green at a
    // known position) + set yellows. Auto-greens are unambiguous so they count; auto-yellows
    // are not counted, which is what prevents over-counting duplicates.
    const presentByRow = [];
    for (let r = 0; r < board.length; r++) {
        const row = board[r] || [];
        const rc = (colors && colors[r]) || [];
        const present = new Map();
        for (let p = 0; p < row.length; p++) {
            const letter = row[p];
            if (!letter) continue;
            const color = rc[p];
            const isUnset = color === TILE_UNSET || color === undefined;
            const showsGreen = color === TILE_GREEN || (isUnset && known.get(p) === letter);
            if (showsGreen) present.set(letter, (present.get(letter) || 0) + 1);
        }
        yellowsByRow[r].forEach((count, letter) => present.set(letter, (present.get(letter) || 0) + count));
        presentByRow.push(present);
    }

    const letters = new Set();
    greenPositions.forEach((_, l) => letters.add(l));
    presentByRow.forEach(m => m.forEach((_, l) => letters.add(l)));
    grayCountByRow.forEach(m => m.forEach((_, l) => letters.add(l)));

    const minCount = new Map();
    const maxCount = new Map();
    letters.forEach(letter => {
        let min = greenPositions.has(letter) ? greenPositions.get(letter).size : 0;
        presentByRow.forEach(m => { if ((m.get(letter) || 0) > min) min = m.get(letter); });
        minCount.set(letter, min);

        // A gray only bounds the count when that letter is fully colored in the row.
        // If a blank occurrence remains, the guess is incomplete (the blank could be the
        // present copy), so it proves neither a maximum nor absence.
        const caps = [];
        grayCountByRow.forEach((gc, r) => {
            const grays = gc.get(letter) || 0;
            if (grays <= 0) return;
            const total = (board[r] || []).filter(x => x === letter).length;
            const present = presentByRow[r].get(letter) || 0;
            const blank = total - present - grays;
            if (blank === 0) caps.push(present);
        });
        let max = null;
        if (caps.length) {
            max = Math.min(...caps);
            if (max < min) max = null; // contradiction: present wins
        }
        maxCount.set(letter, max);
    });

    return { known, excluded, minCount, maxCount };
}

/**
 * Produces the display color for every tile. Set tiles show exactly what the user
 * chose (always toggleable). Unset tiles are auto-colored from the facts, but ONLY
 * for unambiguous cases: absent letters and the known green position of a letter
 * that is not (yet) a known duplicate. Once a letter is known to occur 2+ times,
 * its tiles are left blank for the user to color, since surplus-vs-extra cannot be
 * inferred.
 */
export function recolorBoard(board, colors, facts = deriveGlobalState(board, colors)) {
    const { known, excluded, minCount, maxCount } = facts;

    return board.map((row, r) => (row || []).map((letter, p) => {
        if (!letter) return TILE_DEFAULT;
        const color = colors && colors[r] ? colors[r][p] : TILE_UNSET;
        if (color != null) return color; // explicit user choice (null/undefined = unset)

        // A known green position is unambiguous, so propagate it even for duplicates.
        if (known.get(p) === letter) return TILE_GREEN;
        if (maxCount.get(letter) === 0) return TILE_GRAY; // absent

        const min = minCount.get(letter) || 0;
        // Once a letter is a known duplicate, its non-green occurrences are ambiguous
        // (surplus vs. another copy), so leave them blank for the user to color.
        if (min >= 2) return TILE_DEFAULT;

        const notHere = (excluded.get(p) && excluded.get(p).has(letter)) ||
            (known.has(p) && known.get(p) !== letter);
        if (min >= 1 && notHere) return TILE_YELLOW; // present, not at this position

        return TILE_DEFAULT;
    }));
}

/**
 * Applies a color the user clicked and resolves the simple consistency rules so the
 * board does not persist obvious contradictions:
 *  - one letter per green position,
 *  - marking a letter present anywhere clears its stale "absent" grays,
 *  - graying a position clears a same-letter green claimed there.
 * Returns a new colors grid. Duplicate-specific surplus is intentionally left alone.
 */
export function applyColor(board, colors, r, p, color) {
    const next = colors.map(row => row.slice());
    const letter = board[r][p];
    next[r][p] = color;

    // For a known-duplicate letter the cross-row rules are ambiguous and would let a
    // click in one word rewrite another (e.g. graying GEESE's surplus E retracting
    // VERSE's green E). So only set the clicked tile and leave the rest of the board.
    const { minCount } = deriveGlobalState(board, next);
    if ((minCount.get(letter) || 0) >= 2) {
        return next;
    }

    const eachCell = (fn) => {
        for (let rr = 0; rr < board.length; rr++) {
            for (let pp = 0; pp < (board[rr] || []).length; pp++) {
                fn(rr, pp);
            }
        }
    };

    if (color === TILE_GREEN) {
        // one letter per position
        eachCell((rr, pp) => {
            if (pp === p && rr !== r && board[rr][pp] && board[rr][pp] !== letter && next[rr][pp] === TILE_GREEN) {
                next[rr][pp] = TILE_UNSET;
            }
        });
    }

    if (color === TILE_GREEN || color === TILE_YELLOW) {
        // present wins: clear "absent" grays of this letter (rows with no present copy)
        for (let rr = 0; rr < board.length; rr++) {
            let rowHasPresent = false;
            for (let pp = 0; pp < (board[rr] || []).length; pp++) {
                if (board[rr][pp] === letter && (next[rr][pp] === TILE_GREEN || next[rr][pp] === TILE_YELLOW)) {
                    rowHasPresent = true;
                    break;
                }
            }
            if (!rowHasPresent) {
                for (let pp = 0; pp < (board[rr] || []).length; pp++) {
                    if (board[rr][pp] === letter && next[rr][pp] === TILE_GRAY) next[rr][pp] = TILE_UNSET;
                }
            }
        }
    }

    if (color === TILE_GRAY) {
        // graying a position retracts any green claimed for the same letter there
        eachCell((rr, pp) => {
            if (pp === p && board[rr][pp] === letter && next[rr][pp] === TILE_GREEN) {
                next[rr][pp] = TILE_UNSET;
            }
        });
    }

    return next;
}

/**
 * Letters available on the keyboard: everything except those proven absent.
 */
export function deriveAvailableLetters(board, colors, facts = deriveGlobalState(board, colors)) {
    const { maxCount } = facts;
    return new Set(ALLOWABLE_CHARACTERS.split("").filter(letter => maxCount.get(letter) !== 0));
}

/**
 * Builds the backend restriction string from the set-tile facts:
 *   <letter><green positions><^min><$max><!excluded positions>   (1-based positions)
 */
export function generateRestrictionString(board, colors, facts = deriveGlobalState(board, colors)) {
    const { known, excluded, minCount, maxCount } = facts;

    const greensByLetter = new Map();
    known.forEach((letter, p) => {
        if (!greensByLetter.has(letter)) greensByLetter.set(letter, new Set());
        greensByLetter.get(letter).add(p);
    });
    const excludedByLetter = new Map();
    excluded.forEach((set, p) => set.forEach(letter => {
        if (!excludedByLetter.has(letter)) excludedByLetter.set(letter, new Set());
        excludedByLetter.get(letter).add(p);
    }));

    let restrictionString = "";
    ALLOWABLE_CHARACTERS.split("").forEach(letter => {
        const max = maxCount.has(letter) ? maxCount.get(letter) : null;
        if (max === 0) return; // absent

        restrictionString += letter;

        const greens = greensByLetter.get(letter);
        if (greens) [...greens].sort((a, b) => a - b).forEach(p => { restrictionString += (p + 1); });

        const min = minCount.get(letter) || 0;
        if (min >= 2) restrictionString += "^" + min;
        if (max !== null && max >= 1) restrictionString += "$" + max;

        const excludedSet = excludedByLetter.get(letter);
        if (excludedSet) {
            const positions = [...excludedSet].filter(p => known.get(p) !== letter).sort((a, b) => a - b);
            if (positions.length) {
                restrictionString += "!";
                positions.forEach(p => { restrictionString += (p + 1); });
            }
        }
    });
    return restrictionString;
}

export function generateAnagramString(board) {
    let anagramString = "";

    for(let i = 0; i < board.length ; i++) {
        for(let j = 0; j < board[i].length; j++) {
            if(board[i][j] !== '') {
                anagramString += board[i][j];
            }
        }
    }

    return anagramString;
}

export function generateConfigParams(boardState) {
    let hardMode = boardState.settings.hardMode ?
        "hardMode=true" : "hardMode=false";

    let requireAnswer = boardState.settings.requireAnswer ?
        "&requireAnswer=true" : "&requireAnswer=false";

    let wordConfig = "&wordLength=" + boardState.settings.wordLength + "&wordList=" + boardState.settings.dictionary + "&wordConfig=" + boardState.settings.wordConfig;

    return hardMode + requireAnswer + wordConfig;
}
