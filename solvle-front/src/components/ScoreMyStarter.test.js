import React from 'react';
import { screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { renderWithContext } from '../testUtils';
import ScoreMyStarter from './ScoreMyStarter';

describe('ScoreMyStarter', () => {
    let alertSpy;

    beforeEach(() => {
        alertSpy = jest.spyOn(window, 'alert').mockImplementation(() => {});
        global.fetch = jest.fn(() =>
            Promise.resolve({
                ok: true,
                json: () => Promise.resolve({
                    tuple: [{ word: 'arise' }],
                    partitionStats: { entropy: 4.123, wordsRemaining: 7.85 },
                }),
            })
        );
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    test('strips whitespace and digits from input as the user types', () => {
        renderWithContext(<ScoreMyStarter />);
        fireEvent.click(screen.getByRole('button', { name: /score starter/i }));

        const input = screen.getByPlaceholderText(/starting word/i);
        fireEvent.change(input, { target: { value: 'a r 1i s e' } });

        expect(input.value).toBe('arise');
    });

    test('rejects input that does not match the 5-letter comma-separated pattern', () => {
        renderWithContext(<ScoreMyStarter />);
        fireEvent.click(screen.getByRole('button', { name: /score starter/i }));

        fireEvent.change(screen.getByPlaceholderText(/starting word/i), { target: { value: 'abc' } });
        fireEvent.click(screen.getByRole('button', { name: /^score my starter$/i }));

        expect(alertSpy).toHaveBeenCalledWith(expect.stringMatching(/5-character words/));
        expect(global.fetch).not.toHaveBeenCalled();
    });

    test('valid input fetches /solvle/scoreTuple and renders formatted entropy and remaining words', async () => {
        renderWithContext(<ScoreMyStarter />);
        fireEvent.click(screen.getByRole('button', { name: /score starter/i }));

        fireEvent.change(screen.getByPlaceholderText(/starting word/i), { target: { value: 'arise' } });
        fireEvent.click(screen.getByRole('button', { name: /^score my starter$/i }));

        await waitFor(() => expect(global.fetch).toHaveBeenCalled());
        expect(global.fetch.mock.calls[0][0]).toContain('/solvle/scoreTuple/arise');

        expect(await screen.findByText('4.12')).toBeInTheDocument();
        expect(screen.getByText('7.85')).toBeInTheDocument();
    });

    test('accepts a comma-separated tuple of 5-letter words', async () => {
        renderWithContext(<ScoreMyStarter />);
        fireEvent.click(screen.getByRole('button', { name: /score starter/i }));

        fireEvent.change(screen.getByPlaceholderText(/starting word/i), { target: { value: 'arise,pound' } });
        fireEvent.click(screen.getByRole('button', { name: /^score my starter$/i }));

        await waitFor(() => expect(global.fetch).toHaveBeenCalled());
        expect(global.fetch.mock.calls[0][0]).toContain('/solvle/scoreTuple/arise,pound');
        expect(alertSpy).not.toHaveBeenCalled();
    });
});
