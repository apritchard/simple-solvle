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

describe('Letter', () => {
    test('renders the underlying letter value', () => {
        const { container } = renderWithContext(<Letter letterPos={0} attemptVal={0} />, {
            boardState: buildBoardState(),
            displayColors: [['', '', '', '', '']],
        });

        expect(container.querySelector('.letter').textContent).toBe('C');
    });

    test('applies the tile color class from displayColors', () => {
        const { container } = renderWithContext(<Letter letterPos={0} attemptVal={0} />, {
            boardState: buildBoardState(),
            displayColors: [['correct', '', '', '', '']],
        });

        expect(container.querySelector('.letter')).toHaveClass('correct');
    });

    test('clicking an entered cell cycles its tile color', () => {
        const { context, container } = renderWithContext(<Letter letterPos={0} attemptVal={0} />, {
            boardState: buildBoardState(),
            displayColors: [['', '', '', '', '']],
        });

        container.querySelector('.letter').click();

        expect(context.cycleTileColor).toHaveBeenCalledWith(0, 0);
    });

    test('clicking a letter in a future row is ignored', () => {
        const { context, container } = renderWithContext(<Letter letterPos={0} attemptVal={2} />, {
            boardState: {
                ...buildBoardState(),
                board: [['', '', '', '', ''], ['', '', '', '', ''], ['C', '', '', '', '']],
                currAttempt: { attempt: 0, letter: 0 },
            },
            displayColors: [['', '', '', '', ''], ['', '', '', '', ''], ['', '', '', '', '']],
        });

        container.querySelector('.letter').click();

        expect(context.cycleTileColor).not.toHaveBeenCalled();
    });

    test('clicking an empty cell is ignored', () => {
        const { context, container } = renderWithContext(<Letter letterPos={1} attemptVal={0} />, {
            boardState: buildBoardState(),
            displayColors: [['', '', '', '', '']],
        });

        container.querySelector('.letter').click();

        expect(context.cycleTileColor).not.toHaveBeenCalled();
    });
});
