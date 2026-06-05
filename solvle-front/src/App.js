/**
 * Wordle base started from the Wordle-Clone-React tutorial, available on github:
 * https://github.com/machadop1407/Wordle-Clone-React
 */

import "./App.css";
import Board from "./components/Board";
import Keyboard from "./components/Keyboard";
import React, {useMemo, useState} from "react";
import Options from "./components/Options";
import {MdHelp} from 'react-icons/md';
import SolvleAlert from "./components/SolvleAlert";
import Config from "./components/Config";
import AppContext from "./contexts/contexts";
import BoardActions from "./components/BoardActions";
import {
    applyColor,
    computeFeedback,
    deriveAvailableLetters,
    deriveGlobalState,
    generateConfigParams,
    generateRestrictionString,
    initialTileColors,
    nextTileColor,
    recolorBoard,
    TILE_DEFAULT,
    TILE_GRAY,
    TILE_UNSET
} from "./functions/functions";

function App() {

    const initialBoard = (rows, width) => {
        let retArray = [];
        for (let i = 0; i < rows; i++) {
            retArray[i] = [];
            for (let j = 0; j < width; j++) {
                retArray[i][j] = "";
            }
        }
        return retArray;
    }

    const initialBoardState = (rows, width) => {
        return {
            board: initialBoard(rows, width),
            currAttempt: {
                attempt: 0,
                letter: 0
            },
            settings: {
                wordLength: width,
                attempts: rows,
                results: 50,
                autoColorWord: "",
                hardMode: localStorage.getItem("hardMode") === 'true',
                requireAnswer: localStorage.getItem("requireAnswer") === 'true',
                usePartitioning: localStorage.getItem("usePartitioning") === 'true',
                rateEnteredWords: localStorage.getItem("rateEnteredWords") === 'true',
                displayEntropy: localStorage.getItem("displayEntropy") === 'true',
                wordConfig: localStorage.getItem("wordConfig") || "SIMPLE",
                dictionary: localStorage.getItem("dictionary") || "SIMPLE"
            },
            shouldUpdate: false
        }
    }

    const [boardState, setBoardState] = useState(initialBoardState(6, 5));

    const initialOptions = () => {
        return {
            wordList: new Set(),
            fishingWords: new Set(),
            bestWords: new Set(),
            wordsWithCharacter: new Map(),
            totalWords: 0,
            knownPositions: new Set()
        }
    }

    const [tileColors, setTileColors] = useState(
        initialTileColors(boardState.settings.attempts, boardState.settings.wordLength));
    const [currentOptions, setCurrentOptions] = useState(initialOptions());
    const [solverOpen, setSolverOpen] = useState(false);
    const [rowScores, setRowScores] = useState([])

    // tileColors holds only the tiles the user has explicitly set (others are
    // TILE_UNSET). displayColors auto-colors the unset tiles from board-wide facts
    // (propagation) for rendering; availableLetters greys keys proven absent. Facts
    // and the restriction string derive from tileColors (set tiles) only.
    //
    // By design we allow transient "invalid" boards mid-edit (e.g. a letter shown
    // gray in one word while set present in another) rather than fighting the user's
    // clicks across words. The derivation resolves contradictions (present wins,
    // greens beat stale exclusions) so the restriction string is always sensible.
    // Derive the board-wide facts once and share them with the display and keyboard memos.
    const facts = useMemo(
        () => deriveGlobalState(boardState.board, tileColors),
        [boardState.board, tileColors]);

    const displayColors = useMemo(
        () => recolorBoard(boardState.board, tileColors, facts),
        [boardState.board, tileColors, facts]);

    const availableLetters = useMemo(
        () => deriveAvailableLetters(boardState.board, tileColors, facts),
        [boardState.board, tileColors, facts]);

    const resetTileColors = (rows, width) => {
        setTileColors(initialTileColors(rows, width));
    }

    const resetBoard = (rows, width) => {
        setBoardState(initialBoardState(rows, width));
        resetTileColors(rows, width);
        setCurrentOptions(initialOptions());
        setRowScores([]);
    }

    // Immutably set a single tile's color.
    const setTileColor = (row, pos, color) => {
        setTileColors(prev => prev.map((r, ri) =>
            ri === row ? r.map((c, ci) => (ci === pos ? color : c)) : r));
    }

    // Cycle a tile from the color the user currently sees, recording it as an
    // explicit set tile (always freely toggleable), and resolve simple conflicts.
    const cycleTileColor = (row, pos) => {
        const shown = (displayColors[row] && displayColors[row][pos]) || TILE_DEFAULT;
        const next = nextTileColor(shown);
        setTileColors(prev => applyColor(boardState.board, prev, row, pos, next));
    }

    // "Exclude All" marks every entered tile gray (absent).
    const setAllUnavailable = () => {
        setTileColors(prev => prev.map((r, ri) =>
            r.map((c, ci) => (boardState.board[ri][ci] !== "" ? TILE_GRAY : c))));
    }

    const setAutoColorSolution = (solution) => {
        setBoardState(prev => ({
            ...prev,
            settings: {
                ...prev.settings,
                autoColorWord: solution
            }
        }));
        resetTileColors(boardState.settings.attempts, boardState.settings.wordLength);
        colorAllWordsBasedOnSolution(solution);
    }

    // Clearing/replacing a board cell drops the user's color for it, returning the
    // tile to the unset (auto-colored) state.
    const clearPosition = (attempt, pos) => {
        if (boardState.board[attempt][pos] === '') {
            return;
        }
        setTileColor(attempt, pos, TILE_UNSET);
    }

    const updateWordRating = () => {
        if (boardState.settings.rateEnteredWords) {

            let restrictionString = generateRestrictionString(boardState.board, tileColors);
            let configParams = generateConfigParams(boardState);

            let currentWord = boardState.board[boardState.currAttempt.attempt].join("");

            // encodeURIComponent so the ^ (min) and $ (max) frequency tokens survive
            // the path; Tomcat rejects a raw ^ with a 400.
            fetch('/solvle/score/' + encodeURIComponent(restrictionString) + "/" + currentWord + "?" + configParams)
                .then(res => {
                    if (res.ok) {
                        return res.json()
                    }
                    throw new Error(res.statusMessage);
                })
                .then((data) => {
                    let newRowScores = rowScores;
                    newRowScores[boardState.currAttempt.attempt] = data;
                    setRowScores([...newRowScores]);
                }).catch(e => {
                    console.log("Error loading word score for " + currentWord);
            });
        }
    }

    const colorCurrentWordBasedOnSolution = () => {
        colorWordBasedOnSolution(boardState.currAttempt.attempt, boardState.settings.autoColorWord);
    }

    const colorAllWordsBasedOnSolution = (solution) => {
        for(let i = 0; i < boardState.currAttempt.attempt; i++) {
            colorWordBasedOnSolution(i, solution);
        }
    }

    const colorWordBasedOnSolution = (attempt, solution) => {
        if (!solution) {
            return;
        }
        // Use true two-pass Wordle feedback so duplicate letters guessed more
        // often than they occur are marked surplus-gray, not all present.
        const guess = boardState.board[attempt].join("");
        const colors = computeFeedback(guess, solution);
        setTileColors(prev => prev.map((r, ri) => (ri === attempt ? [...colors] : r)));
    }

    const onEnter = () => {
        if (boardState.currAttempt.letter !== boardState.settings.wordLength) {
            return;
        }
        updateWordRating();
        colorCurrentWordBasedOnSolution();
        setBoardState(prev => ({
            ...prev,
            board: prev.board,
            currAttempt: {
                attempt: boardState.currAttempt.attempt + 1,
                letter: 0
            }
        }));

    };

    const onDelete = () => {
        if (boardState.currAttempt.attempt === 0 && boardState.currAttempt.letter === 0) {
            return;
        }
        if (boardState.currAttempt.letter === 0) {
            setBoardState( prev => ({
                ...prev,
                board: prev.board,
                currAttempt: {
                    attempt: boardState.currAttempt.attempt - 1,
                    letter: boardState.settings.wordLength
                }
            }));
            let newRowScores = rowScores;
            newRowScores[boardState.currAttempt.attempt - 1] = null;
            setRowScores([...newRowScores]);
            return;
        }
        clearPosition(boardState.currAttempt.attempt, boardState.currAttempt.letter - 1);
        const newBoard = [...boardState.board];
        newBoard[boardState.currAttempt.attempt][boardState.currAttempt.letter - 1] = "";
        setBoardState(prev => ({
            ...prev,
            board: newBoard,
            currAttempt: {
                attempt: boardState.currAttempt.attempt,
                letter: boardState.currAttempt.letter - 1
            }
        }));
    };

    const onSelectLetter = (key) => {
        if(boardState.currAttempt.letter >= boardState.settings.wordLength) {
            return;
        }
        const newBoard = [...boardState.board];
        newBoard[boardState.currAttempt.attempt][boardState.currAttempt.letter] = key;
        let newLetter = Math.min(boardState.currAttempt.letter + 1, boardState.settings.wordLength);
        setBoardState(prev => ({
            ...prev,
            board: newBoard,
            currAttempt: {
                attempt: boardState.currAttempt.attempt,
                letter: newLetter
            }
        }));
    };

    const onSelectWord = (word) => {
        if(boardState.currAttempt.attempt >= boardState.settings.attempts) {
            return;
        }
        const newBoard = [...boardState.board];
        for (let i = 0; i < word.length; i++) {
            clearPosition(boardState.currAttempt.attempt, i);
            newBoard[boardState.currAttempt.attempt][i] = word[i];
        }
        updateWordRating();
        colorCurrentWordBasedOnSolution();
        setBoardState(prev => ({
            ...prev,
            board: newBoard,
            currAttempt: {
                attempt: Math.min(boardState.currAttempt.attempt + 1, boardState.settings.attempts),
                letter: 0
            }
        }));
    }

    const helpText = <div><div>Solvle is a tool for evaluating potential solutions to Wordle puzzles or evaluating
        your performance after finishing a game.</div>
        <ul>
            <li>Press "Set Solution" if you already know the answer so that Solvle can color your tiles automatically.</li>
            <li>Tap a word on the right, type letters, or tap the on-screen keyboard to enter a word.</li>
            <li>If you want to change the coloring, click the letters you've entered to mark them gray (unavailable), yellow (required, but wrong position), or green (correct position).</li>
            <li>If you've manually entered a word, press ENTER to advance the word choice to the next line.</li>
            <li>Viable words appear on the right. You can click a word to fill in its letters on the current line.</li>
            <li>Numbers under each letter on the keyboard indicate how many of the available words include that letter.</li>
            <li>Fish🐟 words help you 'fish' for new letters without trying to reuse existing letters.</li>
            <li>Cut✂ words help you 'cut' the remaining options as much as possible.</li>
        </ul>

        Click the gear icon to customize your options.

    </div>

    return (
        <AppContext.Provider
            value={{
                boardState,
                setBoardState,
                currentOptions,
                setCurrentOptions,
                availableLetters,
                tileColors,
                displayColors,
                cycleTileColor,
                solverOpen,
                setSolverOpen,
                rowScores,
                onSelectLetter,
                onDelete,
                onEnter,
                setAllUnavailable,
                onSelectWord,
                resetBoard,
                setAutoColorSolution
            }}
        >
            <div className="App">
                <nav>
                    <div className="header">
                        <h1>🔍Solvle</h1>
                        <SolvleAlert heading="Welcome to Solvle - a Word Puzzle Analysis Tool"
                                     message={helpText}
                                     persist={true}
                                     persistMessage={"?"}
                                     persistVariant={"dark"}/>
                        <Config/>
                        <div className="helpIcon">
                            <a href=" https://github.com/apritchard/simple-solvle">
                                <MdHelp title="This is a toy project built to learn React. Source code available at https://github.com/apritchard/simple-solvle"/>
                            </a>
                        </div>
                    </div>
                </nav>

                <div className="game">
                    <BoardActions />
                    <div className="parent">
                        <Board/>
                        <Options/>
                    </div>
                    <Keyboard/>
                </div>
            </div>
        </AppContext.Provider>
    );
}

export default App;
