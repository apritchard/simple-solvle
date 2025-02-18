import React, {useContext, useState, useRef} from 'react';
import AppContext from "../contexts/contexts";
import {Button} from "react-bootstrap";
import SolveModal from "./SolveModal";
import AutoColor from "./AutoColor";
import RateMyGame from "./RateMyGame";
import UtilityDropdown from "./UtilityDropdown";
import TupleCompletion from "./TupleCompletion";
import ScoreMyStarter from "./ScoreMyStarter";

function BoardActions() {

    const {
        boardState,
        resetBoard,
        setAllUnavailable
    } = useContext(AppContext);

    const releaseFocus = (e) => {
        if (e && e.target) {
            e.target.blur();
        }
    }

    const clickReset = (e) => {
        resetBoard(boardState.settings.attempts, boardState.settings.wordLength);
        releaseFocus(e);
    }

    const excludeCurrent = (e) => {
        setAllUnavailable();
        releaseFocus(e);
    }


    return (
        <div>
            <Button title="clear all letters from the board" variant="dark" onClick={clickReset}>Reset Board</Button>
            <Button title="set all letters unavailable" variant="dark" onClick={excludeCurrent}>Exclude All</Button>
            <AutoColor />
            <UtilityDropdown>
                <SolveModal />
                <RateMyGame />
                <TupleCompletion />
                <ScoreMyStarter />
            </UtilityDropdown>
        </div>
    );
}

export default BoardActions;