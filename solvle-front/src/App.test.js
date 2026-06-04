import {render, screen, waitFor} from '@testing-library/react';
import '@testing-library/jest-dom';
import App from './App';

beforeEach(() => {
    global.fetch?.resetMocks?.();
    global.fetch = jest.fn(() => Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
            restrictionString: '',
            wordList: [],
            fishingWords: [],
            bestWords: [],
            totalWords: 0,
            wordsWithCharacter: {},
            knownPositions: []
        })
    }));
});

afterEach(() => {
    jest.restoreAllMocks();
});

test('renders the app and loads initial suggestions', async () => {
    render(<App />);

    expect(screen.getByRole('heading', {name: /Solvle/i})).toBeInTheDocument();

    await waitFor(() => {
        expect(global.fetch).toHaveBeenCalledWith(expect.stringMatching(/^\/solvle\//));
    });
    expect(await screen.findByText('0 possible words')).toBeInTheDocument();
});
