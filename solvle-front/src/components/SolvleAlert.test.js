import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import SolvleAlert from './SolvleAlert';

describe('SolvleAlert', () => {
    beforeEach(() => {
        localStorage.clear();
    });

    test('renders the alert when localStorage helpSeen2 is unset', () => {
        render(
            <SolvleAlert
                heading="Hi"
                message="Welcome message"
                persist={true}
                persistMessage="?"
                persistVariant="dark"
            />
        );

        expect(screen.getByText('Welcome message')).toBeInTheDocument();
    });

    test('hides the alert and shows the persist button after the user dismisses it', () => {
        render(
            <SolvleAlert
                heading="Hi"
                message="Welcome message"
                persist={true}
                persistMessage="?"
                persistVariant="dark"
            />
        );

        fireEvent.click(screen.getByRole('button', { name: /close/i }));

        expect(screen.queryByText('Welcome message')).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: '?' })).toBeInTheDocument();
        expect(localStorage.getItem('helpSeen2')).toBe('true');
    });

    test('clicking the persist button restores the alert', () => {
        localStorage.setItem('helpSeen2', 'true');
        render(
            <SolvleAlert
                heading="Hi"
                message="Welcome message"
                persist={true}
                persistMessage="?"
                persistVariant="dark"
            />
        );

        // initially hidden
        expect(screen.queryByText('Welcome message')).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: '?' }));

        expect(screen.getByText('Welcome message')).toBeInTheDocument();
        expect(localStorage.getItem('helpSeen2')).toBe('false');
    });

    test('renders nothing when alert is dismissed and persist is false', () => {
        localStorage.setItem('helpSeen2', 'true');
        const { container } = render(
            <SolvleAlert
                heading="Hi"
                message="Welcome message"
                persist={false}
            />
        );

        expect(container).toBeEmptyDOMElement();
    });
});
