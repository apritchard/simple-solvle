/**
 * Wordle base started from the Wordle-Clone-React tutorial, available on github:
 * https://github.com/machadop1407/Wordle-Clone-React
 */

import "./App.css";
import Board from "./components/Board";
import Keyboard from "./components/Keyboard";
import React, {useState} from "react";
import Options from "./components/Options";
import {MdHelp} from 'react-icons/md';
import SolvleAlert from "./components/SolvleAlert";
import Config from "./components/Config";
import AppContext from "./contexts/contexts";
import BoardActions from "./components/BoardActions";
import {ALLOWABLE_CHARACTERS, generateConfigParams, generateRestrictionString} from "./functions/functions";

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

    const initialAvailableLetters = () => {
        return new Set(ALLOWABLE_CHARACTERS.split(""));
    }

    const initialKnownLetters = (width) => {
        let ret = new Map();
        for (let i = 0; i < width; i++) {
            ret.set(i, "");
        }
        return ret;
    }

    const initialUnsureLetters = (width) => {
        let ret = new Map();
        for (let i = 0; i < width; i++) {
            ret.set(i, new Set());
        }
        return ret;
    }

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

    const [availableLetters, setAvailableLetters] = useState(initialAvailableLetters());
    const [knownLetters, setKnownLetters] = useState(initialKnownLetters(boardState.settings.wordLength));
    const [unsureLetters, setUnsureLetters] = useState(initialUnsureLetters(boardState.settings.wordLength));
    const [currentOptions, setCurrentOptions] = useState(initialOptions());
    const [solverOpen, setSolverOpen] = useState(false);
    const [rowScores, setRowScores] = useState([])

    const resetLetterInfo = (width) => {
        setAvailableLetters(initialAvailableLetters());
        setUnsureLetters(initialUnsureLetters(width))
        setKnownLetters(initialKnownLetters(width));
    }

    const resetBoard = (rows, width) => {
        setBoardState(initialBoardState(rows, width));
        resetLetterInfo(width);
        setCurrentOptions(initialOptions());
        setRowScores([]);
    }

    const addKnownLetter = (pos, letter) => {
        setKnownLetters(prev => new Map(prev.set(pos, letter)));
    }

    const removeKnownLetter = (pos, letter) => {
        if (knownLetters.get(pos) === letter) {
            setKnownLetters(prev => new Map(prev.set(pos, "")));
        }
    }

    const addUnsureLetter = (pos, letter) => {
        setUnsureLetters(prev => new Map(prev.set(pos, prev.get(pos).add(letter))));
    }

    const removeUnsureLetter = (pos, letter) => {
        setUnsureLetters(prev => {
            prev.get(pos).delete(letter);
            return new Map(unsureLetters.set(pos, unsureLetters.get(pos)));
        });
    }

    const addAvailableLetter = (letter) => {
        setAvailableLetters(prev => new Set([...prev]).add(letter));
    }

    const removeAvailableLetter = (letter) => {
        setAvailableLetters(prev => {
            prev.delete(letter);
            return new Set([...prev]);
        });
    }

    const setAllUnavailable = () => {
        for(let i=0; i < boardState.board.length; i++) {
            for(let j=0; j < boardState.board[i].length; j++) {
                removeAvailableLetter(boardState.board[i][j]);
            }
        }
        setUnsureLetters(initialUnsureLetters(boardState.settings.wordLength))
        setKnownLetters(initialKnownLetters(boardState.settings.wordLength));

    }

    const setAutoColorSolution = (solution) => {
        setBoardState(prev => ({
            ...prev,
            settings: {
                ...prev.settings,
                autoColorWord: solution
            }
        }));
        resetLetterInfo(boardState.settings.wordLength);
        colorAllWordsBasedOnSolution(solution);
    }

    const clearPosition = (attempt, pos, replacementLetter) => {
        //get letter
        let oldLetter = boardState.board[attempt][pos];

        //if replacement letter same as old letter, bail
        if (oldLetter === replacementLetter || oldLetter === '') {
            return;
        }

        //if old letter in same position on a different word, make no changes
        for (let row = 0; row < boardState.currAttempt.attempt; row++) {
            if (boardState.board[row][pos] === oldLetter) {
                console.log("letter " + oldLetter + " found in attempt " + row + ", making no updates");
                return;
            }
        }

        console.log("Removing " + oldLetter + " from unsure list for position " + pos);
        removeUnsureLetter(pos, oldLetter);

        console.log("Clearing position " + pos + " of known letter " + oldLetter);
        removeKnownLetter(pos, oldLetter);

        //if old letter is anywhere on the board, leave its available status alone
        for (let row = 0; row < boardState.currAttempt.attempt; row++) {
            for (let x = 0; x < boardState.settings.wordLength; x++) {
                if (boardState.board[row][x] === oldLetter) {
                    console.log("Letter " + oldLetter + " elsewhere on board, not updating its availability");
                    return;
                }
            }
        }

        if (!availableLetters.has(oldLetter)) {
            console.log("Restoring " + oldLetter + " to availability list");
            availableLetters.add(oldLetter);
        }

    }

    const updateWordRating = () => {
        if (boardState.settings.rateEnteredWords) {

            let restrictionString = generateRestrictionString(availableLetters, knownLetters, unsureLetters);
            let configParams = generateConfigParams(boardState);

            let currentWord = "";
            boardState.board[boardState.currAttempt.attempt].map(letter => {currentWord+= letter});

            fetch('/solvle/' + restrictionString + "/" + currentWord + "?" + configParams)
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
        solution = solution.toUpperCase()
        console.log("Coloring attempt " + attempt + " for " + solution);
        if(solution) {
            boardState.board[attempt].map((letter, idx) => {
                if(solution.charAt(idx) == letter) {
                    console.log("adding " + letter + " known for " + idx);
                    addKnownLetter(idx, letter);
                } else if (solution.includes(letter)) {
                    console.log("adding " + letter + " unsure for " + idx);
                    addUnsureLetter(idx, letter);
                } else {
                    console.log("removing available " + letter);
                    removeAvailableLetter(letter);
                }
            });
        }
    }

    const onEnter = () => {
        if (boardState.currAttempt.letter !== boardState.settings.wordLength) {
            return;
        }
        console.log("Updating board state");
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
            console.log("At top of board, cannot delete");
            return;
        }
        if (boardState.currAttempt.letter === 0) {
            console.log("Returning to previous line");
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
            console.log("No room for new letters");
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
            console.log("No room for more words");
            return;
        }
        console.log("Setting " + word + " " + word.length + " " + boardState.currAttempt.attempt + " " + boardState.currAttempt.letter);
        const newBoard = [...boardState.board];
        for (let i = 0; i < word.length; i++) {
            clearPosition(boardState.currAttempt.attempt, i, word[i]);
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
                knownLetters,
                unsureLetters,
                solverOpen,
                setSolverOpen,
                rowScores,
                onSelectLetter,
                onDelete,
                onEnter,
                addKnownLetter,
                removeKnownLetter,
                addUnsureLetter,
                removeUnsureLetter,
                addAvailableLetter,
                removeAvailableLetter,
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