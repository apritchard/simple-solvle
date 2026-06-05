import React from 'react';
import '@testing-library/jest-dom';
import { renderWithContext } from '../testUtils';
import Letter from './Letter';

const buildBoardState = (overrides = {}) => ({
    board: [['C', '', '', '', '']],
    currAttempt: { attempt: 0, letter: 1 },
    settings: { wordLength: 5, attempts: 6, dictionary: 'SIMPLE', wordConfig: 'SIMPLE' },
    shouldUpdate: false,
    ...overrides,
});

const emptyKnownLetters = (width = 5) => {
    const m = new Map();
    for (let i = 0; i < width; i++) m.set(i, '');
    return m;
};

const emptyUnsureLetters = (width = 5) => {
    const m = new Map();
    for (let i = 0; i < width; i++) m.set(i, new Set());
    return m;
};

describe('Letter', () => {
    test('renders the underlying letter value', () => {
        const { container } = renderWithContext(<Letter letterPos={0} attemptVal={0} />, {
            boardState: buildBoardState(),
            availableLetters: new Set(['C']),
            knownLetters: emptyKnownLetters(),
            unsureLetters: emptyUnsureLetters(),
        });

        expect(container.querySelector('.letter').textContent).toBe('C');
    });

    test('default state click removes the letter from availability and clears known position', () => {
        const { context, container } = renderWithContext(<Letter letterPos={0} attemptVal={0} />, {
            boardState: buildBoardState(),
            availableLetters: new Set(['C']),
            knownLetters: emptyKnownLetters(),
            unsureLetters: emptyUnsureLetters(),
        });

        container.querySelector('.letter').click();

        expect(context.removeKnownLetter).toHaveBeenCalledWith(0, 'C');
        expect(context.removeAvailableLetter).toHaveBeenCalledWith('C');
    });

    test('error state click promotes the letter to unsure', () => {
        const { context, container } = renderWithContext(<Letter letterPos={0} attemptVal={0} />, {
            boardState: buildBoardState(),
            availableLetters: new Set(),
            knownLetters: emptyKnownLetters(),
            unsureLetters: emptyUnsureLetters(),
        });

        container.querySelector('.letter').click();

        expect(context.addAvailableLetter).toHaveBeenCalledWith('C');
        expect(context.addUnsureLetter).toHaveBeenCalledWith(0, 'C');
    });

    test('unsure state click promotes the letter to known', () => {
        const unsure = emptyUnsureLetters();
        unsure.get(0).add('C');
        const { context, container } = renderWithContext(<Letter letterPos={0} attemptVal={0} />, {
            boardState: buildBoardState(),
            availableLetters: new Set(['C']),
            knownLetters: emptyKnownLetters(),
            unsureLetters: unsure,
        });

        container.querySelector('.letter').click();

        expect(context.removeUnsureLetter).toHaveBeenCalledWith(0, 'C');
        expect(context.addKnownLetter).toHaveBeenCalledWith(0, 'C');
    });

    test('known state click clears the known position', () => {
        const known = emptyKnownLetters();
        known.set(0, 'C');
        const { context, container } = renderWithContext(<Letter letterPos={0} attemptVal={0} />, {
            boardState: buildBoardState(),
            availableLetters: new Set(['C']),
            knownLetters: known,
            unsureLetters: emptyUnsureLetters(),
        });

        container.querySelector('.letter').click();

        expect(context.removeKnownLetter).toHaveBeenCalledWith(0, 'C');
    });

    test('clicking a letter in a future row is ignored', () => {
        const { context, container } = renderWithContext(<Letter letterPos={0} attemptVal={2} />, {
            boardState: {
                ...buildBoardState(),
                board: [['', '', '', '', ''], ['', '', '', '', ''], ['C', '', '', '', '']],
                currAttempt: { attempt: 0, letter: 0 },
            },
            availableLetters: new Set(['C']),
            knownLetters: emptyKnownLetters(),
            unsureLetters: emptyUnsureLetters(),
        });

        container.querySelector('.letter').click();

        expect(context.removeKnownLetter).not.toHaveBeenCalled();
        expect(context.removeAvailableLetter).not.toHaveBeenCalled();
        expect(context.addUnsureLetter).not.toHaveBeenCalled();
        expect(context.addKnownLetter).not.toHaveBeenCalled();
    });

    test('clicking an empty cell is ignored', () => {
        const { context, container } = renderWithContext(<Letter letterPos={1} attemptVal={0} />, {
            boardState: buildBoardState(),
            availableLetters: new Set(),
            knownLetters: emptyKnownLetters(),
            unsureLetters: emptyUnsureLetters(),
        });

        container.querySelector('.letter').click();

        expect(context.removeKnownLetter).not.toHaveBeenCalled();
        expect(context.addUnsureLetter).not.toHaveBeenCalled();
    });
});
