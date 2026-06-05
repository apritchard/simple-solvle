import React from 'react';
import '@testing-library/jest-dom';
import { renderWithContext } from '../testUtils';
import OptionTab from './OptionTab';

const buildBoardState = (settingsOverrides = {}) => ({
    board: [['', '', '', '', '']],
    currAttempt: { attempt: 0, letter: 0 },
    settings: {
        wordLength: 5, attempts: 6, dictionary: 'SIMPLE', wordConfig: 'SIMPLE',
        hardMode: false, requireAnswer: false, usePartitioning: false,
        rateEnteredWords: false, displayEntropy: false,
        ...settingsOverrides,
    },
    shouldUpdate: false,
});

describe('OptionTab', () => {
    test('renders the heading and one list item per word', () => {
        const wordList = [
            { word: 'crane', freqScore: 0.92 },
            { word: 'slate', freqScore: 0.88 },
        ];

        const { container, getByText } = renderWithContext(
            <OptionTab wordList={wordList} onSelectWord={jest.fn()} heading="2 possible words" />,
            { boardState: buildBoardState() }
        );

        expect(getByText('2 possible words')).toBeInTheDocument();
        expect(container.querySelectorAll('li.optionItem')).toHaveLength(2);
    });

    test('formats freqScore as a percentage when partitionStats is absent', () => {
        const wordList = [{ word: 'crane', freqScore: 0.92 }];

        const { container } = renderWithContext(
            <OptionTab wordList={wordList} onSelectWord={jest.fn()} heading="" />,
            { boardState: buildBoardState() }
        );

        expect(container.querySelector('li').textContent).toContain('92%');
    });

    test('shows wordsRemaining (1 decimal) when displayEntropy is false and partitionStats present', () => {
        const wordList = [{
            word: 'crane',
            freqScore: 0.92,
            partitionStats: { wordsRemaining: 7.45, entropy: 3.2, ruts: [] },
        }];

        const { container } = renderWithContext(
            <OptionTab wordList={wordList} onSelectWord={jest.fn()} heading="" />,
            { boardState: buildBoardState({ displayEntropy: false }) }
        );

        expect(container.querySelector('li').textContent).toContain('7.5');
    });

    test('shows entropy (2 decimals) when displayEntropy is true', () => {
        const wordList = [{
            word: 'crane',
            freqScore: 0.92,
            partitionStats: { wordsRemaining: 7.45, entropy: 3.21, ruts: [] },
        }];

        const { container } = renderWithContext(
            <OptionTab wordList={wordList} onSelectWord={jest.fn()} heading="" />,
            { boardState: buildBoardState({ displayEntropy: true }) }
        );

        expect(container.querySelector('li').textContent).toContain('3.21');
    });

    test('applies rutDetected class when partitionStats.ruts is non-empty', () => {
        const wordList = [
            { word: 'crane', freqScore: 0.92, partitionStats: { wordsRemaining: 5, entropy: 2, ruts: ['_at_e'] } },
            { word: 'slate', freqScore: 0.88, partitionStats: { wordsRemaining: 4, entropy: 2.1, ruts: [] } },
        ];

        const { container } = renderWithContext(
            <OptionTab wordList={wordList} onSelectWord={jest.fn()} heading="" />,
            { boardState: buildBoardState() }
        );

        const items = container.querySelectorAll('li.optionItem');
        expect(items[0]).toHaveClass('rutDetected');
        expect(items[1]).not.toHaveClass('rutDetected');
    });

    test('bolds solution-list words when also present in solutionList', () => {
        const wordList = [{ word: 'crane', freqScore: 0.92 }, { word: 'pound', freqScore: 0.5 }];
        const solutionList = [{ word: 'crane' }];

        const { container } = renderWithContext(
            <OptionTab wordList={wordList} solutionList={solutionList} onSelectWord={jest.fn()} heading="" />,
            { boardState: buildBoardState() }
        );

        const items = container.querySelectorAll('li.optionItem');
        expect(items[0].querySelector('strong')).not.toBeNull();
        expect(items[1].querySelector('strong')).toBeNull();
    });

    test('clicking a list item calls onSelectWord with the uppercased word', () => {
        const onSelectWord = jest.fn();
        const wordList = [{ word: 'crane', freqScore: 0.92 }];

        const { container } = renderWithContext(
            <OptionTab wordList={wordList} onSelectWord={onSelectWord} heading="" />,
            { boardState: buildBoardState() }
        );

        container.querySelector('li').click();

        expect(onSelectWord).toHaveBeenCalledWith('CRANE');
    });

    test('renders at most 100 list items', () => {
        const wordList = Array.from({ length: 150 }, (_, i) => ({ word: 'w' + i, freqScore: 0.5 }));

        const { container } = renderWithContext(
            <OptionTab wordList={wordList} onSelectWord={jest.fn()} heading="" />,
            { boardState: buildBoardState() }
        );

        expect(container.querySelectorAll('li.optionItem')).toHaveLength(100);
    });
});
