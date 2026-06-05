import React, {useContext} from "react";
import AppContext from "../contexts/contexts";

function Letter({letterPos, attemptVal}) {
    const {boardState, displayColors, cycleTileColor} = useContext(AppContext);

    const letter = boardState.board[attemptVal][letterPos];
    // displayColors holds the derived CSS class for each tile: "" | "error" | "almost" | "correct"
    const letterState = (displayColors[attemptVal] && displayColors[attemptVal][letterPos]) || "";
    let highlight = boardState.currAttempt.attempt === attemptVal && boardState.currAttempt.letter === letterPos;

    const toggleState = () => {
        if (attemptVal > boardState.currAttempt.attempt || letter === "") {
            return;
        }
        cycleTileColor(attemptVal, letterPos);
    }

    return (
        <div className={"letter" + (letterState ? " " + letterState : "") + (highlight ? " highlightLetter" : "")} onClick={toggleState}>
            {letter}
        </div>
    );
}

export default Letter;
