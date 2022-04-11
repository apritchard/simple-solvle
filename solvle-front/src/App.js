/**
 * Wordle base started from the Wordle-Clone-React tutorial, available on github:
 * https://github.com/machadop1407/Wordle-Clone-React
 */

import "./App.css";
import Board from "./components/Board";
import Keyboard from "./components/Keyboard";
import React, {useState, createContext} from "react";
import Options from "./components/Options";

export const AppContext = createContext();

function App() {
    const [availableLetters, setAvailableLetters] = useState(new Set("ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("")));
    const [knownLetters, setKnownLetters] = useState(() => {
        let ret = new Map();
        for (let i = 0; i < 5; i++) {
            ret.set(i, "");
        }
        return ret;
    });
    const [unsureLetters, setUnsureLetters] = useState(() => {
        let ret = new Map();
        for (let i = 0; i < 5; i++) {
            ret.set(i, new Set());
        }
        return ret;
    });
    const [currentOptions, setCurrentOptions] = useState(new Set());

    const [board, setBoard] = useState([
        ["", "", "", "", ""],
        ["", "", "", "", ""],
        ["", "", "", "", ""],
        ["", "", "", "", ""],
        ["", "", "", "", ""],
        ["", "", "", "", ""],
    ]);
    const [currAttempt, setCurrAttempt] = useState({attempt: 0, letter: 0});

    const addKnownLetter = (pos, letter) => {
        setKnownLetters(prev => new Map(prev.set(pos, letter)));
    }

    const removeKnownLetter = (pos) => {
        setKnownLetters(prev => new Map(prev.set(pos, "")));
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

    const onEnter = () => {
        if (currAttempt.letter !== 5) return;

        let currWord = "";
        for (let i = 0; i < 5; i++) {
            currWord += board[currAttempt.attempt][i];
        }
        setCurrAttempt({attempt: currAttempt.attempt + 1, letter: 0});
    };

    const onDelete = () => {
        if (currAttempt.letter === 0) return;
        const newBoard = [...board];
        newBoard[currAttempt.attempt][currAttempt.letter - 1] = "";
        setBoard(newBoard);
        setCurrAttempt({...currAttempt, letter: currAttempt.letter - 1});
    };

    const onSelectLetter = (key) => {
        if (currAttempt.letter > 4) return;
        const newBoard = [...board];
        newBoard[currAttempt.attempt][currAttempt.letter] = key;
        setBoard(newBoard);
        setCurrAttempt({
            attempt: currAttempt.attempt,
            letter: currAttempt.letter + 1,
        });
    };

    const onSelectWord = (word) => {
        console.log("Setting " + word + word.length + " " + currAttempt.attempt + currAttempt.letter);
        if(currAttempt.letter !== 0) {
            return;
        }
        const newBoard = [...board];
        for(let i = 0; i < word.length; i++) {
            newBoard[currAttempt.attempt][i] = word[i];
        }
        setBoard(newBoard);
        setCurrAttempt( {
            attempt: currAttempt.attempt,
            letter: word.length
        });
    }

    return (
        <div className="App">
            <nav>
                <h1>Solvle</h1>
            </nav>
            <AppContext.Provider
                value={{
                    board,
                    setBoard,
                    currAttempt,
                    setCurrAttempt,
                    onSelectLetter,
                    onDelete,
                    onEnter,
                    availableLetters,
                    knownLetters,
                    unsureLetters,
                    addKnownLetter,
                    removeKnownLetter,
                    addUnsureLetter,
                    removeUnsureLetter,
                    addAvailableLetter,
                    removeAvailableLetter,
                    currentOptions,
                    setCurrentOptions,
                    onSelectWord
                }}
            >
                <div className="game">
                    <div className="parent">
                        <Board/>
                        <Options/>
                    </div>
                    <Keyboard/>
                </div>
            </AppContext.Provider>
        </div>
    );
}

export default App;