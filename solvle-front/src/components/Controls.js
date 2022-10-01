import React, {useContext} from 'react';
import {Button} from 'react-bootstrap';
import {MDBRange, MDBSwitch, MDBTooltip} from 'mdb-react-ui-kit';
import AppContext from "../contexts/contexts";

function Controls() {
    const {
        boardState,
        setBoardState,
        dictionary,
        setDictionary,
    } = useContext(AppContext);

    const toggleResetWords = (event) => {
        setDictionary(event.target.value);
    }

    const updateSetting = (e) => {
        localStorage.setItem(e.target.name, e.target.checked);
        setBoardState(prev => ({
            ...prev,
            settings: {
                ...prev.settings,
                [e.target.name]: e.target.checked
            }
        }));
    }

    const setPreset = (e) => {
        localStorage.setItem("wordConfig", e.target.value);
        setBoardState(prev => ({
            ...prev,
            settings: {
                ...prev.settings,
                wordConfig: e.target.value
            }
        }));
    }

    const hardModeHelpText = "Limit word suggestions to words that are available in hard mode."
    const rateEnteredWordsHelpText = "Show fishing score and average words remaining for each word you enter. Calculates when you press ENTER using the currently selected letters, so" +
        " it will only be accurate if you mark the state of your previous words, but not your current word, before pressing enter.";
    const biasHelpText = "Calculation Bias enables Solvle to prioritize words that match the" +
        " positions of letters. Customize the extent of this prioritization using the sliders below.";

    const simpleHelpText = "Does not employ letter position heuristics and disables partitioning."
    const lowestAverageHelpText = "Produces the best average score (about 3.457) and never fails a puzzle.";
    const lowestMaxHelpText = "Finishes all puzzles in 5 or fewer guesses, but worse average performance (about 3.54816).";
    const most2HelpText = "Achieves a score of 2 on about 6.5% of puzzles (about twice as often as other strategies), but can fail if used past word 2. Average score 3.55982.";
    const most3HelpText = "Maximizes scores of 3 or lower and does not fail, but scores 6 on more words. Averages 3.46781.";


    return (
        <div className="controls">
            <div onChange={toggleResetWords} className="wordLists">
                Word List:
                <span
                    title="2315 words: https://github.com/techtribeyt/Wordle/blob/main/wordle_answers.txt Defaults to Scrabble for words that are not 5-letters.">
                    <input id="simpleRadio" defaultChecked={dictionary === "simple"} type="radio" value="simple"
                           name="dict"/>
                    <label htmlFor="simpleRadio">Simple</label>
                </span>
                <span
                    title="1906 words: Same as simple, but has had solutions used before Aug 9, 2022 removed">
                    <input id="reducedRadio" defaultChecked={dictionary === "reduced"} type="radio" value="reduced"
                           name="dict"/>
                    <label htmlFor="reducedRadio">Reduced</label>
                </span>
                <span title="172820 words: https://github.com/dolph/dictionary/blob/master/enable1.txt">
                    <input id="bigRadio" defaultChecked={dictionary === "big"} type="radio" value="big" name="dict"/>
                    <label htmlFor="bigRadio">Scrabble</label>
                </span>
            </div>
            <hr/>
            <div>
                <div title={hardModeHelpText} >
                    <MDBSwitch id='hardModeSwitch' label="Hard Mode" name="hardMode"
                               defaultChecked={boardState.settings.hardMode} onChange={updateSetting}/>
                </div>
                <div title={rateEnteredWordsHelpText}>
                    <MDBSwitch id='enableWordRatingSwitch' label="Rate Words As You Enter" name="rateEnteredWords"
                               defaultChecked={boardState.settings.rateEnteredWords} onChange={updateSetting}/>
                </div>
            </div>

            <hr/>

            <h6 title={biasHelpText}>Heuristic Strategy</h6>
            <div onChange={setPreset} className="wordLists">
                <div
                    title={simpleHelpText}>
                    <input id="simpleStratRadio" defaultChecked={boardState.settings.wordConfig === "SIMPLE"} type="radio" value="SIMPLE"
                           name="strat"/>
                    <label htmlFor="simpleStratRadio">Simple</label>
                </div>
                <div
                    title={lowestAverageHelpText}>
                    <input id="optimalMeanRadio" defaultChecked={boardState.settings.wordConfig === "OPTIMAL_MEAN"} type="radio" value="OPTIMAL_MEAN"
                           name="strat"/>
                    <label htmlFor="optimalMeanRadio">Optimal Mean</label>
                </div>
                <div
                    title={lowestMaxHelpText}>
                    <input id="lowestMaxRadio" defaultChecked={boardState.settings.wordConfig === "LOWEST_MAX"} type="radio" value="LOWEST_MAX"
                           name="strat"/>
                    <label htmlFor="optimalMeanRadio">Lowest Max</label>
                </div>
                <div
                    title={most2HelpText}>
                    <input id="most2Radio" defaultChecked={boardState.settings.wordConfig === "TWO_OR_LESS"} type="radio" value="TWO_OR_LESS"
                           name="strat"/>
                    <label htmlFor="most2Radio">Most Twos</label>
                </div>
                <div
                    title={most3HelpText}>
                    <input id="most3Radio" defaultChecked={boardState.settings.wordConfig === "THREE_OR_LESS"} type="radio" value="THREE_OR_LESS"
                           name="strat"/>
                    <label htmlFor="most3Radio">Most Threes</label>
                </div>
            </div>
        </div>
    );
}

export default Controls;