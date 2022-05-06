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
                useBias: localStorage.getItem("useBias") === 'true',
                usePartitioning: localStorage.getItem("usePartitioning") === 'true',
                calculationConfig: {
                    rightLocationMultiplier: localStorage.getItem("rightLocationMultiplier") || 4,
                    uniquenessMultiplier: localStorage.getItem("uniquenessMultiplier") || 9,
                    useHarmonic: false,
                    partitionThreshold: localStorage.getItem("partitionThreshold") !== null ? localStorage.getItem("partitionThreshold") : 30,
                    fishingThreshold: 2,
                    viableWordPreference: localStorage.getItem("viableWordPreference") !== null ? localStorage.getItem("viableWordPreference") : .007
                }
            },
            shouldUpdate: false
        }
    }

    const [boardState, setBoardState] = useState(initialBoardState(6, 5));

    const initialAvailableLetters = () => {
        return new Set("ABCDEFGHIJKLMNOPQRSTUVWXYZ".split(""));
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
        }
    }

    const [availableLetters, setAvailableLetters] = useState(initialAvailableLetters());
    const [knownLetters, setKnownLetters] = useState(initialKnownLetters(boardState.settings.wordLength));
    const [unsureLetters, setUnsureLetters] = useState(initialUnsureLetters(boardState.settings.wordLength));
    const [currentOptions, setCurrentOptions] = useState(initialOptions());
    const [dictionary, setDictionary] = useState("simple");
    const [solverOpen, setSolverOpen] = useState(false);

    const resetBoard = (rows, width) => {
        setBoardState(initialBoardState(rows, width));
        setAvailableLetters(initialAvailableLetters());
        setUnsureLetters(initialUnsureLetters(width))
        setKnownLetters(initialKnownLetters(width));
        setCurrentOptions(initialOptions());
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

    const onEnter = () => {
        if (boardState.currAttempt.letter !== boardState.settings.wordLength) {
            return;
        }

        console.log("Updating board state");
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
        console.log("Setting " + word + word.length + " " + boardState.currAttempt.attempt + boardState.currAttempt.letter);
        const newBoard = [...boardState.board];
        for (let i = 0; i < word.length; i++) {
            clearPosition(boardState.currAttempt.attempt, i, word[i]);
            newBoard[boardState.currAttempt.attempt][i] = word[i];
        }
        setBoardState(prev => ({
            ...prev,
            board: newBoard,
            currAttempt: {
                attempt: Math.min(boardState.currAttempt.attempt + 1, boardState.settings.attempts-1),
                letter: 0
            }
        }));
    }

    const helpText = <div><div>Solvle is a tool that helps evaluate potential candidate words based on available letters.
        One way to use it is to type in words you tried in other puzzle games and mark letters as known or unavailable.
        The list of words on the right will update to reflect which words are still available. Words are ranked based
        on how many of their letters are shared by other available words.</div>
        <ul>
            <li>Type letters or tap the on-screen keyboard to enter a word.</li>
            <li>Click the letters you've entered to mark them gray (unavailable), yellow (required, but wrong position), or green (correct position).</li>
            <li>Press ENTER to advance the word choice to the next line.</li>
            <li>The top 100 viable words appear on the right. You can click a word to fill in its letters on the current line.</li>
            <li>Numbers under each letter on the keyboard indicate how many of the available words include that letter.</li>
            <li>Fishing words help you 'fish' for new letters without trying to reuse existing letters.</li>
            <li>Click the gear wheel to enable more complex recommendation options.</li>
        </ul></div>

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
                dictionary,
                setDictionary,
                solverOpen,
                setSolverOpen,
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
                resetBoard
            }}
        >
            <div className="App">
                <nav>
                    <div className="header">
                        <h1>Solvle</h1>
                        <SolvleAlert heading="Welcome to Solvle - a Word Puzzle Analysis Tool"
                                     message={helpText}
                                     persist={true}
                                     persistMessage={"?"}
                                     persistVariant={"dark"}/>
                        <Config/>
                        <div className="helpIcon">
                            <MdHelp
                                title="This is a toy project built to learn React. Source code available at https://github.com/apritchard/solvle"/>
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