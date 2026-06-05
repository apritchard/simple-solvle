import React from 'react';
import { screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { renderWithContext } from '../testUtils';
import SolveModal from './SolveModal';

describe('SolveModal', () => {
    beforeEach(() => {
        global.fetch = jest.fn(() =>
            Promise.resolve({
                ok: true,
                json: () => Promise.resolve(['CRANE', 'SLATE', 'CRATE']),
            })
        );
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    test('opening the modal toggles solverOpen on context', () => {
        const { context } = renderWithContext(<SolveModal />);

        fireEvent.click(screen.getByRole('button', { name: /solve word/i }));

        expect(context.setSolverOpen).toHaveBeenCalledWith(true);
    });

    test('submitting the form fetches /solvle/solve with config params and renders the returned guesses', async () => {
        renderWithContext(<SolveModal />, {
            boardState: {
                board: [['', '', '', '', '']],
                currAttempt: { attempt: 0, letter: 0 },
                settings: {
                    wordLength: 5, attempts: 6, dictionary: 'SIMPLE', wordConfig: 'SIMPLE',
                    hardMode: true, requireAnswer: false,
                },
                shouldUpdate: false,
            },
        });

        fireEvent.click(screen.getByRole('button', { name: /solve word/i }));

        fireEvent.change(screen.getByPlaceholderText(/answer to today/i), { target: { value: 'crane' } });
        fireEvent.change(screen.getByPlaceholderText(/starting word/i), { target: { value: 'slate' } });
        fireEvent.click(screen.getByRole('button', { name: /solve!/i }));

        await waitFor(() => expect(global.fetch).toHaveBeenCalled());
        expect(global.fetch.mock.calls[0][0]).toContain('/solvle/solve/crane');
        expect(global.fetch.mock.calls[0][0]).toContain('hardMode=true');
        expect(global.fetch.mock.calls[0][0]).toContain('wordList=SIMPLE');
        expect(global.fetch.mock.calls[0][0]).toContain('firstWord=slate');

        expect(await screen.findByText('CRANE')).toBeInTheDocument();
        expect(screen.getByText('SLATE')).toBeInTheDocument();
        expect(screen.getByText('CRATE')).toBeInTheDocument();
    });

    test('closing the modal clears solverOpen', () => {
        const { context } = renderWithContext(<SolveModal />);

        fireEvent.click(screen.getByRole('button', { name: /solve word/i }));
        // Modal renders both an X close button (aria-label="Close") and a footer "Close" button;
        // click the footer text button explicitly to disambiguate.
        const closeButtons = screen.getAllByRole('button', { name: /close/i });
        fireEvent.click(closeButtons[closeButtons.length - 1]);

        expect(context.setSolverOpen).toHaveBeenLastCalledWith(false);
    });
});
