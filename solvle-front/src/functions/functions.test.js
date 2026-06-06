import {
    ALLOWABLE_CHARACTERS,
    applyColor,
    computeFeedback,
    deriveAvailableLetters,
    generateAnagramString,
    generateConfigParams,
    generateRestrictionString,
    nextTileColor,
    recolorBoard,
    TILE_DEFAULT,
    TILE_GRAY,
    TILE_GREEN,
    TILE_UNSET,
    TILE_YELLOW,
} from './functions';

// convenience builders
const row = (...colors) => colors;
const unsetRow = (n = 5) => new Array(n).fill(TILE_UNSET);

describe('nextTileColor', () => {
    test('cycles default -> gray -> yellow -> green -> default', () => {
        expect(nextTileColor(TILE_DEFAULT)).toBe(TILE_GRAY);
        expect(nextTileColor(TILE_GRAY)).toBe(TILE_YELLOW);
        expect(nextTileColor(TILE_YELLOW)).toBe(TILE_GREEN);
        expect(nextTileColor(TILE_GREEN)).toBe(TILE_DEFAULT);
    });
});

describe('computeFeedback', () => {
    test('all greens for an exact match', () => {
        expect(computeFeedback('CRANE', 'CRANE'))
            .toEqual([TILE_GREEN, TILE_GREEN, TILE_GREEN, TILE_GREEN, TILE_GREEN]);
    });

    test('marks surplus duplicate letters gray (GEESE vs CRANE)', () => {
        expect(computeFeedback('GEESE', 'CRANE'))
            .toEqual([TILE_GRAY, TILE_GRAY, TILE_GRAY, TILE_GRAY, TILE_GREEN]);
    });

    test('distributes duplicate yellows up to the solution count (LLAMA vs BALLS)', () => {
        expect(computeFeedback('LLAMA', 'BALLS'))
            .toEqual([TILE_YELLOW, TILE_YELLOW, TILE_YELLOW, TILE_GRAY, TILE_GRAY]);
    });
});

describe('deriveAvailableLetters', () => {
    test('all letters available for a blank board', () => {
        const available = deriveAvailableLetters([['', '', '', '', '']], [unsetRow()]);
        expect(available.size).toBe(ALLOWABLE_CHARACTERS.length);
    });

    test('a fully-absent (gray-only) letter is no longer available', () => {
        const board = [['B', '', '', '', '']];
        const colors = [row(TILE_GRAY, TILE_UNSET, TILE_UNSET, TILE_UNSET, TILE_UNSET)];
        const available = deriveAvailableLetters(board, colors);
        expect(available.has('B')).toBe(false);
        expect(available.has('A')).toBe(true);
    });

    test('a capped-but-present letter stays available', () => {
        const board = [['E', 'E', 'A', 'R', 'T']];
        const colors = [row(TILE_GREEN, TILE_GRAY, TILE_UNSET, TILE_UNSET, TILE_UNSET)];
        const available = deriveAvailableLetters(board, colors);
        expect(available.has('E')).toBe(true);
    });
});

describe('generateRestrictionString', () => {
    test('emits every allowable letter for a blank board', () => {
        const result = generateRestrictionString([['', '', '', '', '']], [unsetRow()]);
        expect(result).toBe(ALLOWABLE_CHARACTERS);
    });

    test('drops fully-absent letters from the pool', () => {
        const board = [['B', '', '', '', '']];
        const colors = [row(TILE_GRAY, TILE_UNSET, TILE_UNSET, TILE_UNSET, TILE_UNSET)];
        const result = generateRestrictionString(board, colors);
        expect(result).not.toContain('B');
        expect(result).toContain('A');
    });

    test('appends 1-based green positions', () => {
        const board = [['C', 'R', 'A', 'N', 'E']];
        const colors = [row(TILE_GREEN, TILE_UNSET, TILE_UNSET, TILE_UNSET, TILE_UNSET)];
        expect(generateRestrictionString(board, colors)).toContain('C1');
    });

    test('emits ! and excluded position for a yellow letter', () => {
        const board = [['C', 'R', 'A', 'N', 'E']];
        const colors = [row(TILE_YELLOW, TILE_UNSET, TILE_UNSET, TILE_UNSET, TILE_UNSET)];
        expect(generateRestrictionString(board, colors)).toContain('C!1');
    });

    test('emits ^min for a double yellow (at least two copies)', () => {
        const board = [['E', 'E', 'A', 'B', 'C']];
        const colors = [row(TILE_YELLOW, TILE_YELLOW, TILE_UNSET, TILE_UNSET, TILE_UNSET)];
        expect(generateRestrictionString(board, colors)).toContain('E^2!12');
    });

    test('emits exact count when a surplus gray bounds a duplicate (GEESE vs VERSE)', () => {
        // SEEDY -> YGYXX (E present twice), then GEESE manually colored as feedback for VERSE:
        // E green@2 and @5, surplus gray@3 -> exactly two E's at positions 2 and 5.
        const board = [
            ['S', 'E', 'E', 'D', 'Y'],
            ['G', 'E', 'E', 'S', 'E'],
        ];
        const colors = [
            row(TILE_YELLOW, TILE_GREEN, TILE_YELLOW, TILE_GRAY, TILE_GRAY),
            row(TILE_GRAY, TILE_GREEN, TILE_GRAY, TILE_GREEN, TILE_GREEN),
        ];
        const result = generateRestrictionString(board, colors);
        expect(result).toContain('E25^2$2!3'); // E at pos 2 and 5, exactly two, not at pos 3
        expect(result).toContain('S4!1');       // S at pos 4, not at pos 1
        expect(result).not.toContain('G');      // absent
    });

    test('counts three copies without inventing a max (GEESE answer, ELIDE then ELEDE)', () => {
        // GEESE has three E's. ELIDE -> YXXXG, ELEDE -> YXGXG. The final E of ELEDE is left as
        // the AUTO-green (unset) propagated from ELIDE's known pos-5 green; it must still count,
        // so the minimum is three. No surplus gray, so no max.
        const board = [
            ['E', 'L', 'I', 'D', 'E'],
            ['E', 'L', 'E', 'D', 'E'],
        ];
        const colors = [
            row(TILE_YELLOW, TILE_GRAY, TILE_GRAY, TILE_GRAY, TILE_GREEN),
            row(TILE_YELLOW, TILE_GRAY, TILE_GREEN, TILE_GRAY, TILE_UNSET), // last E auto-green
        ];
        const result = generateRestrictionString(board, colors);
        expect(result).toContain('E35^3!1'); // E at pos 3 and 5, at least three, not at pos 1
        expect(result).not.toContain('E35^3$'); // no max asserted
    });

    test('counts auto-greens toward an exact duplicate bound (VERSE greens propagated into GEESE)', () => {
        // VERSE sets both E greens; in GEESE those E's are auto-green (unset) and only the
        // surplus middle E is grayed. The auto-greens must count, bounding E to exactly two.
        const board = [
            ['V', 'E', 'R', 'S', 'E'],
            ['G', 'E', 'E', 'S', 'E'],
        ];
        const colors = [
            row(TILE_UNSET, TILE_GREEN, TILE_UNSET, TILE_UNSET, TILE_GREEN),
            row(TILE_UNSET, TILE_UNSET, TILE_GRAY, TILE_UNSET, TILE_UNSET),
        ];
        expect(generateRestrictionString(board, colors)).toContain('E25^2$2!3');
    });

    test('bounds a single-occurrence surplus duplicate to exactly one (GEESE vs CRANE)', () => {
        const board = [['G', 'E', 'E', 'S', 'E']];
        const colors = [row(TILE_GRAY, TILE_GRAY, TILE_GRAY, TILE_GRAY, TILE_GREEN)];
        expect(generateRestrictionString(board, colors)).toContain('E5$1!23');
    });

    test('preserves multilingual letters in ALLOWABLE order', () => {
        const result = generateRestrictionString([['', '', '', '', '']], [unsetRow()]);
        expect(result.indexOf('ẞ')).toBeLessThan(result.indexOf('Ö'));
    });
});

describe('recolorBoard (propagation of unambiguous facts only)', () => {
    const slateSpate = [
        ['S', 'L', 'A', 'T', 'E'],
        ['S', 'P', 'A', 'T', 'E'],
    ];

    test('an absent letter greys across every row', () => {
        const raw = [row(TILE_GRAY, TILE_UNSET, TILE_UNSET, TILE_UNSET, TILE_UNSET), unsetRow()];
        const display = recolorBoard(slateSpate, raw);
        expect(display[0][0]).toBe(TILE_GRAY);
        expect(display[1][0]).toBe(TILE_GRAY); // propagated
    });

    test('a non-duplicate known green position greens the same position in other rows', () => {
        const raw = [row(TILE_UNSET, TILE_UNSET, TILE_GREEN, TILE_UNSET, TILE_UNSET), unsetRow()];
        const display = recolorBoard(slateSpate, raw);
        expect(display[0][2]).toBe(TILE_GREEN);
        expect(display[1][2]).toBe(TILE_GREEN); // propagated
    });

    test('editing one row reflects in another without re-clicking', () => {
        const raw = [
            row(TILE_GRAY, TILE_GRAY, TILE_GREEN, TILE_YELLOW, TILE_GREEN),
            unsetRow(),
        ];
        const display = recolorBoard(slateSpate, raw);
        expect(display[1]).toEqual([TILE_GRAY, TILE_DEFAULT, TILE_GREEN, TILE_YELLOW, TILE_GREEN]);
    });

    test('does not over-assert a present letter at an unknown position in another word', () => {
        const board = [
            ['S', 'L', 'A', 'T', 'E'],
            ['A', 'G', 'E', 'N', 'T'],
        ];
        const raw = [row(TILE_UNSET, TILE_UNSET, TILE_YELLOW, TILE_UNSET, TILE_UNSET), unsetRow()];
        const display = recolorBoard(board, raw);
        expect(display[0][2]).toBe(TILE_YELLOW);
        expect(display[1][0]).toBe(TILE_DEFAULT);
    });

    test('auto-greens a duplicate at its known green positions but leaves extras blank', () => {
        // VERSE has E green at positions 2 and 5 (both set). Typing GEESE should auto-green
        // those positions, but leave the ambiguous middle E blank for the user.
        const board = [
            ['V', 'E', 'R', 'S', 'E'],
            ['G', 'E', 'E', 'S', 'E'],
        ];
        const raw = [
            row(TILE_UNSET, TILE_GREEN, TILE_UNSET, TILE_UNSET, TILE_GREEN),
            unsetRow(),
        ];
        const display = recolorBoard(board, raw);
        expect(display[1][1]).toBe(TILE_GREEN);   // known green position propagates
        expect(display[1][4]).toBe(TILE_GREEN);   // known green position propagates
        expect(display[1][2]).toBe(TILE_DEFAULT); // ambiguous extra left blank
    });

    test('a partially-colored duplicate does not infer absence (GEESE with a blank E)', () => {
        // CRANE left blank; GEESE marked all gray except the final E cleared back to blank.
        // The blank E means E is not proven absent, so CRANE's E must not turn gray.
        const board = [
            ['C', 'R', 'A', 'N', 'E'],
            ['G', 'E', 'E', 'S', 'E'],
        ];
        const colors = [
            unsetRow(),
            row(TILE_GRAY, TILE_GRAY, TILE_GRAY, TILE_GRAY, TILE_DEFAULT),
        ];
        const display = recolorBoard(board, colors);
        expect(display[0][4]).toBe(TILE_DEFAULT); // CRANE's E stays blank
        expect(deriveAvailableLetters(board, colors).has('E')).toBe(true);
    });

    test('a fully all-gray duplicate IS concluded absent', () => {
        const board = [
            ['C', 'R', 'A', 'N', 'E'],
            ['G', 'E', 'E', 'S', 'E'],
        ];
        const colors = [
            unsetRow(),
            row(TILE_GRAY, TILE_GRAY, TILE_GRAY, TILE_GRAY, TILE_GRAY),
        ];
        const display = recolorBoard(board, colors);
        expect(display[0][4]).toBe(TILE_GRAY); // every E gray -> E absent -> propagates
        expect(deriveAvailableLetters(board, colors).has('E')).toBe(false);
    });

    test('a set tile always shows the chosen color (freely toggleable)', () => {
        const board = [['E', 'E', 'A', 'R', 'T']];
        const raw = [row(TILE_GREEN, TILE_GRAY, TILE_UNSET, TILE_UNSET, TILE_UNSET)];
        const display = recolorBoard(board, raw);
        expect(display[0][0]).toBe(TILE_GREEN);
        expect(display[0][1]).toBe(TILE_GRAY);
    });
});

describe('applyColor (consistency on user clicks)', () => {
    const slateSpate = [
        ['S', 'L', 'A', 'T', 'E'],
        ['S', 'P', 'A', 'T', 'E'],
    ];

    test('marking a letter present clears its stale absent gray (present wins)', () => {
        // S is gray (absent) in SLATE; user marks S yellow in SPATE
        const raw = [row(TILE_GRAY, TILE_UNSET, TILE_UNSET, TILE_UNSET, TILE_UNSET), unsetRow()];
        const next = applyColor(slateSpate, raw, 1, 0, TILE_YELLOW);
        expect(next[0][0]).toBe(TILE_UNSET); // the absent gray was cleared
        const display = recolorBoard(slateSpate, next);
        expect(display[0][0]).toBe(TILE_YELLOW); // re-derives as present
        expect(display[1][0]).toBe(TILE_YELLOW);
    });

    test('greening a position retracts a different letter greened there', () => {
        const raw = [row(TILE_UNSET, TILE_GREEN, TILE_UNSET, TILE_UNSET, TILE_UNSET), unsetRow()];
        // row1 col1 is P; greening it should clear row0 col1 (L) green
        const next = applyColor(slateSpate, raw, 1, 1, TILE_GREEN);
        expect(next[0][1]).toBe(TILE_UNSET);
        expect(next[1][1]).toBe(TILE_GREEN);
    });

    test('graying a position retracts a same-letter green claimed there', () => {
        const raw = [row(TILE_GREEN, TILE_UNSET, TILE_UNSET, TILE_UNSET, TILE_UNSET), unsetRow()];
        const next = applyColor(slateSpate, raw, 1, 0, TILE_GRAY); // gray S at col0 in SPATE
        expect(next[0][0]).toBe(TILE_UNSET); // SLATE's S green retracted
    });

    test('keeps a surplus gray when the row has other present copies', () => {
        // GEESE: E green@1 and @4, surplus gray@2 — graying the surplus must not clear the greens
        const board = [['G', 'E', 'E', 'S', 'E']];
        const raw = [row(TILE_UNSET, TILE_GREEN, TILE_UNSET, TILE_UNSET, TILE_GREEN)];
        const next = applyColor(board, raw, 0, 2, TILE_GRAY);
        expect(next[0][1]).toBe(TILE_GREEN);
        expect(next[0][4]).toBe(TILE_GREEN);
        expect(next[0][2]).toBe(TILE_GRAY);
    });

    test('clicking a duplicate letter never rewrites another word', () => {
        // VERSE's E greens are set; graying the surplus E in GEESE must leave VERSE untouched.
        const board = [
            ['V', 'E', 'R', 'S', 'E'],
            ['G', 'E', 'E', 'S', 'E'],
        ];
        const raw = [
            row(TILE_UNSET, TILE_GREEN, TILE_UNSET, TILE_UNSET, TILE_GREEN),
            unsetRow(),
        ];
        const next = applyColor(board, raw, 1, 2, TILE_GRAY); // gray GEESE's middle E
        expect(next[0][1]).toBe(TILE_GREEN); // VERSE E untouched
        expect(next[0][4]).toBe(TILE_GREEN); // VERSE E untouched
        expect(next[1][2]).toBe(TILE_GRAY);
    });
});

describe('generateAnagramString', () => {
    test('flattens a filled board to a single concatenated string', () => {
        const board = [
            ['C', 'R', 'A', 'N', 'E'],
            ['S', 'L', 'A', 'T', 'E'],
        ];
        expect(generateAnagramString(board)).toBe('CRANESLATE');
    });

    test('ignores empty cells', () => {
        const board = [
            ['C', 'R', 'A', '', ''],
            ['', '', '', '', ''],
        ];
        expect(generateAnagramString(board)).toBe('CRA');
    });

    test('returns empty string for a fully blank board', () => {
        const board = [
            ['', '', ''],
            ['', '', ''],
        ];
        expect(generateAnagramString(board)).toBe('');
    });
});

describe('generateConfigParams', () => {
    test('encodes hardMode, requireAnswer, wordLength, wordList, and wordConfig', () => {
        const boardState = {
            settings: {
                hardMode: true,
                requireAnswer: false,
                wordLength: 5,
                dictionary: 'SIMPLE',
                wordConfig: 'OPTIMAL_MEAN_WITH_PARTITIONING'
            }
        };
        expect(generateConfigParams(boardState)).toBe(
            'hardMode=true&requireAnswer=false&wordLength=5&wordList=SIMPLE&wordConfig=OPTIMAL_MEAN_WITH_PARTITIONING'
        );
    });

    test('serializes falsey hardMode and truthy requireAnswer', () => {
        const boardState = {
            settings: {
                hardMode: false,
                requireAnswer: true,
                wordLength: 6,
                dictionary: 'GERMAN_6MAL5',
                wordConfig: 'SIMPLE'
            }
        };
        expect(generateConfigParams(boardState)).toBe(
            'hardMode=false&requireAnswer=true&wordLength=6&wordList=GERMAN_6MAL5&wordConfig=SIMPLE'
        );
    });
});
