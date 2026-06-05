import React from 'react';
import '@testing-library/jest-dom';
import { renderWithContext } from '../testUtils';
import Key from './Key';

describe('Key', () => {
    test('renders the key value and uses the disabled class when not in available letters', () => {
        const { container } = renderWithContext(<Key keyVal="Q" disabled={true} />);

        const keyDiv = container.querySelector('.key');
        expect(keyDiv.textContent).toBe('Q');
        expect(keyDiv).toHaveClass('disabled');
    });

    test('uses the big class for bigKey and suppresses the letter count', () => {
        const { container } = renderWithContext(<Key keyVal="ENTER" bigKey={true} />);

        const keyDiv = container.querySelector('.key');
        expect(keyDiv).toHaveClass('big');
        expect(container.querySelector('.letterAmt').textContent).toBe('');
    });

    test('clicking a letter key calls onSelectLetter with the key value', () => {
        const { context, container } = renderWithContext(<Key keyVal="A" />);

        container.querySelector('.key').click();

        expect(context.onSelectLetter).toHaveBeenCalledWith('A');
        expect(context.onEnter).not.toHaveBeenCalled();
        expect(context.onDelete).not.toHaveBeenCalled();
    });

    test('clicking ENTER routes to onEnter', () => {
        const { context, container } = renderWithContext(<Key keyVal="ENTER" bigKey={true} />);

        container.querySelector('.key').click();

        expect(context.onEnter).toHaveBeenCalled();
        expect(context.onSelectLetter).not.toHaveBeenCalled();
    });

    test('clicking DELETE routes to onDelete', () => {
        const { context, container } = renderWithContext(<Key keyVal="DELETE" bigKey={true} />);

        container.querySelector('.key').click();

        expect(context.onDelete).toHaveBeenCalled();
        expect(context.onSelectLetter).not.toHaveBeenCalled();
    });

    test('shows the count of viable words containing the letter from currentOptions', () => {
        const { container } = renderWithContext(<Key keyVal="A" />, {
            currentOptions: {
                wordList: [], fishingWords: [], bestWords: [], totalWords: 0, knownPositions: [],
                wordsWithCharacter: { a: 42 },
            },
        });

        expect(container.querySelector('.letterAmt').textContent).toBe('42');
    });
});
