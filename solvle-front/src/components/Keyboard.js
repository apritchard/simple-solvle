import React, {useCallback, useEffect, useContext} from "react";
import Key from "./Key";
import AppContext from "../contexts/contexts";

function Keyboard() {
    const keyboardLayouts = {
        keys1: ["Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"],
        keys2: ["A", "S", "D", "F", "G", "H", "J", "K", "L"],
        keys3: ["Z", "X", "C", "V", "B", "N", "M"],
        icelandic: ["Á", "Ð", "É", "Í", "Ó", "Ú", "Ý", "Þ", "Æ", "Ö"],
        spanish: ["A", "S", "D", "F", "G", "H", "J", "K", "L", "Ñ"],
        german1: ["Q", "W", "E", "R", "T", "Z", "U", "I", "O", "P", "Ü"],
        german2: ["A", "S", "D", "F", "G", "H", "J", "K", "L", "Ö", "Ä"],
        german3: ["Y", "X", "C", "V", "B", "N", "M", "ẞ"]
    };

    const {
        availableLetters,
        onSelectLetter,
        onEnter,
        onDelete,
        solverOpen,
        boardState
    } = useContext(AppContext);

    const handleKeyboard = useCallback(
        (event) => {
            if(solverOpen) {
                return;
            }
            if (event.key === "Enter") {
                onEnter();
            } else if (event.key === "Backspace") {
                onDelete();
            } else {
                Object.values(keyboardLayouts).forEach((keysArray) => {
                    keysArray.forEach((key) => {
                        if (event.key.toLowerCase() === key.toLowerCase()) {
                            onSelectLetter(key);
                        }
                    });
                });
            }
        },
        [onDelete, onEnter, onSelectLetter]
    );
    useEffect(() => {
        document.addEventListener("keydown", handleKeyboard);

        return () => {
            document.removeEventListener("keydown", handleKeyboard);
        };
    }, [handleKeyboard]);

    return (
        <div className="keyboard" onKeyDown={handleKeyboard}>
            {boardState.settings.dictionary === 'ICELANDIC' && (
                <div className="keyboardLine">
                    {keyboardLayouts.icelandic.map((key) => {
                        return <Key key={key} keyVal={key} disabled={!availableLetters.has(key)}/>;
                    })}
                </div>
            )}
            <div className="keyboardLine">
                {(boardState.settings.dictionary === 'GERMAN_WORDLE_GLOBAL' ? keyboardLayouts.german1 : keyboardLayouts.keys1).map((key) => (
                    <Key key={key} keyVal={key} disabled={!availableLetters.has(key)} />
                ))}
            </div>
            <div className="keyboardLine">
                {(boardState.settings.dictionary === 'GERMAN_WORDLE_GLOBAL' ? keyboardLayouts.german2 :
                    boardState.settings.dictionary === 'SPANISH' ? keyboardLayouts.spanish : keyboardLayouts.keys2).map((key) => (
                    <Key key={key} keyVal={key} disabled={!availableLetters.has(key)} />
                ))}
            </div>
            <div className="keyboardLine">
                <Key keyVal="ENTER" bigKey />
                {(boardState.settings.dictionary === 'GERMAN_WORDLE_GLOBAL' ? keyboardLayouts.german3 : keyboardLayouts.keys3).map((key) => (
                    <Key key={key} keyVal={key} disabled={!availableLetters.has(key)} />
                ))}
                <Key keyVal={"DELETE"} bigKey/>
            </div>
        </div>
    );
}

export default Keyboard;