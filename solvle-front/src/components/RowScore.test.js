import React from 'react';
import '@testing-library/jest-dom';
import { renderWithContext } from '../testUtils';
import RowScore from './RowScore';

describe('RowScore', () => {
    test('renders fishing %, remaining (1 decimal), and entropy (2 decimals) when rowScores entry is populated', () => {
        const rowScores = [{ fishingScore: 0.83, remainingWords: 4.27, entropy: 3.142 }];

        const { container } = renderWithContext(<RowScore rowNumber={0} />, { rowScores });

        const score = container.querySelector('.rowScore');
        expect(score.textContent).toContain('83%');
        expect(score.textContent).toContain('4.3');
        expect(score.textContent).toContain('3.14');
    });

    test('shows the 🐟✂ header on the first row only', () => {
        const rowScores = [{ fishingScore: 0.5, remainingWords: 3, entropy: 1 }];

        const { container: firstRow } = renderWithContext(<RowScore rowNumber={0} />, { rowScores });
        expect(firstRow.textContent).toContain('🐟✂');

        const { container: secondRow } = renderWithContext(<RowScore rowNumber={1} />, { rowScores });
        expect(secondRow.textContent).not.toContain('🐟✂');
    });

    test('renders empty strings when the rowScores entry is missing', () => {
        const { container } = renderWithContext(<RowScore rowNumber={2} />, { rowScores: [] });

        const score = container.querySelector('.rowScore');
        expect(score).not.toBeNull();
        // Only blank divs inside the rowScore (no % or numeric content rendered)
        expect(score.textContent).toBe('');
    });
});
