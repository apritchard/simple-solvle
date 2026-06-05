import {
    ALLOWABLE_CHARACTERS,
    generateAnagramString,
    generateConfigParams,
    generateRestrictionString
} from './functions';

const allAvailable = () => new Set(ALLOWABLE_CHARACTERS.split(""));

const emptyKnownLetters = (width = 5) => {
    const map = new Map();
    for (let i = 0; i < width; i++) {
        map.set(i, "");
    }
    return map;
};

const emptyUnsureLetters = (width = 5) => {
    const map = new Map();
    for (let i = 0; i < width; i++) {
        map.set(i, new Set());
    }
    return map;
};

describe('generateRestrictionString', () => {
    test('emits every available letter in ALLOWABLE_CHARACTERS order with no positional suffixes', () => {
        const result = generateRestrictionString(allAvailable(), emptyKnownLetters(), emptyUnsureLetters());

        expect(result).toBe(ALLOWABLE_CHARACTERS);
    });

    test('omits letters that are not in availableLetters', () => {
        const available = new Set(['A', 'B', 'C']);

        const result = generateRestrictionString(available, emptyKnownLetters(), emptyUnsureLetters());

        expect(result).toBe('ABC');
    });

    test('appends 1-based position digit for each known letter', () => {
        const available = new Set(['C', 'R', 'A', 'N', 'E']);
        const knownLetters = emptyKnownLetters();
        knownLetters.set(0, 'C');
        knownLetters.set(4, 'E');

        const result = generateRestrictionString(available, knownLetters, emptyUnsureLetters());

        // Letters appear in ALLOWABLE_CHARACTERS order (A, C, E, N, R), known positions appended 1-based
        expect(result).toBe('AC1E5NR');
    });

    test('emits ! prefix followed by 1-based positions for unsure letters', () => {
        const available = new Set(['A', 'B', 'C']);
        const unsure = emptyUnsureLetters();
        unsure.get(1).add('B');
        unsure.get(3).add('B');

        const result = generateRestrictionString(available, emptyKnownLetters(), unsure);

        // B is unsure at positions 1 and 3 (0-based) → ! then 2 then 4 (1-based)
        expect(result).toBe('AB!24C');
    });

    test('combines known and unsure positions on the same letter in backend-compatible order', () => {
        const available = new Set(['A', 'B']);
        const known = emptyKnownLetters();
        known.set(0, 'A');
        const unsure = emptyUnsureLetters();
        unsure.get(2).add('A');

        const result = generateRestrictionString(available, known, unsure);

        // A is known at pos 0 (→1), unsure at pos 2 (→3). Known digits come before !unsure.
        expect(result).toBe('A1!3B');
    });

    test('preserves non-English characters from the multilingual alphabet', () => {
        const available = new Set(['A', 'Á', 'Ö', 'Ñ', 'ẞ']);

        const result = generateRestrictionString(available, emptyKnownLetters(), emptyUnsureLetters());

        // Order follows ALLOWABLE_CHARACTERS, not insertion order — ẞ comes before Ö
        expect(result).toBe('AÁÑẞÖ');
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
