import React from 'react';
import { render } from '@testing-library/react';
import AppContext from './contexts/contexts';

/**
 * Renders a UI inside an AppContext.Provider with sensible defaults.
 * Pass `contextOverrides` to set specific fields (e.g. boardState, rowScores).
 * All callable context fields default to jest.fn() so individual tests can
 * assert what was called without explicitly mocking.
 */
export function renderWithContext(ui, contextOverrides = {}) {
    const defaultContext = {
        boardState: {
            board: [['', '', '', '', '']],
            currAttempt: { attempt: 0, letter: 0 },
            settings: {
                wordLength: 5,
                attempts: 6,
                dictionary: 'SIMPLE',
                wordConfig: 'SIMPLE',
                hardMode: false,
                requireAnswer: false,
                usePartitioning: false,
                rateEnteredWords: false,
                displayEntropy: false,
            },
            shouldUpdate: false,
        },
        setBoardState: jest.fn(),
        currentOptions: {
            wordList: [],
            fishingWords: [],
            bestWords: [],
            wordsWithCharacter: {},
            totalWords: 0,
            knownPositions: [],
        },
        setCurrentOptions: jest.fn(),
        availableLetters: new Set(),
        tileColors: [[null, null, null, null, null]],
        displayColors: [['', '', '', '', '']],
        cycleTileColor: jest.fn(),
        solverOpen: false,
        setSolverOpen: jest.fn(),
        rowScores: [],
        onSelectLetter: jest.fn(),
        onDelete: jest.fn(),
        onEnter: jest.fn(),
        setAllUnavailable: jest.fn(),
        onSelectWord: jest.fn(),
        resetBoard: jest.fn(),
        setAutoColorSolution: jest.fn(),
    };
    const contextValue = { ...defaultContext, ...contextOverrides };
    const renderResult = render(
        <AppContext.Provider value={contextValue}>{ui}</AppContext.Provider>
    );
    return { ...renderResult, context: contextValue };
}
