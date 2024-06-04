import React, {useContext} from 'react';
import {MDBSwitch} from 'mdb-react-ui-kit';
import AppContext from "../contexts/contexts";

function Controls() {
    const {
        boardState,
        setBoardState,
    } = useContext(AppContext);

    const toggleResetWords = (e) => {
        localStorage.setItem("dictionary", e.target.value);
        setBoardState(prev => ({
            ...prev,
            settings: {
                ...prev.settings,
                dictionary: e.target.value
            }
        }))
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
        let usePartitioning = String(e.target.value != "SIMPLE" && e.target.value != "OPTIMAL_MEAN");
        localStorage.setItem("usePartitioning", usePartitioning);
        setBoardState(prev => ({
            ...prev,
            settings: {
                ...prev.settings,
                usePartitioning: usePartitioning,
                wordConfig: e.target.value
            }
        }));
    }

    const hardModeHelpText = "Limit word suggestions to words that are available in hard mode."
    const rateEnteredWordsHelpText = "Show fishing score and average words remaining for each word you enter. Calculates when you press ENTER using the currently selected letters, so" +
        " it will only be accurate if you mark the state of your previous words, but not your current word, before pressing enter.";
    const biasHelpText = "Calculation Bias enables Solvle to prioritize words that match the" +
        " positions of letters. Customize the extent of this prioritization using the sliders below.";

    const simpleHelpText = "Does not employ letter position heuristics and disables partitioning. Mean: 3.7637, StDv: 0.7012, Median: 4.0, Counts: {2=51, 3=740, 4=1243, 5=267, 6=14}"
    const simplePartitionHelpText = "Does not employ letter position heuristics, but enables partitioning. Mean: 3.6307, StDv: 0.6227, Median: 4.0, Counts: {2=62, 3=848, 4=1288, 5=117}"
    const lowestAverageHelpText = "Uses letter position heuristics to minimize the average score. Mean: 3.5965, StDv: 0.6701, Median: 4.0, Counts: {1=1, 2=56, 3=992, 4=1102, 5=155, 6=9}";
    const lowestAveragePartitionHelpText = "Uses letter position heuristics and partitioning to minimize the average score. Mean: 3.4570, StDv: 0.6005, Median: 3.0, Counts: {1=1, 2=71, 3=1168, 4=1019, 5=56}";
    const lowestAverageExtendedPartitionHelpText = "Enable partitioning up to 4000 remaining words. May cause app instability due to slow response.";
    const most2HelpText = "Adjusts heuristics to maximize the odds of getting a score in 2 or 3, but risks failure. Mean: 3.5598, StDv: 0.8170, Median: 4.0, Counts: {1=1, 2=146, 3=996, 4=953, 5=177, 6=33, 7=7, 8=2}";

    const displayEntropyHelpText = "Display Entropy score instead of average words remaining. Entropy is used to sort partition entries by maximum information gain, but may not be a meaningful number to read.";

    return (
        <div className="controls">
            <div onChange={toggleResetWords} className="wordLists">
                Word List:
                <br/>
                <span
                    title="Only valid solutions.">
                    <input id="simpleRadio" defaultChecked={boardState.settings.dictionary === "SIMPLE"} type="radio"
                           value="SIMPLE"
                           name="dict"/>
                    <label htmlFor="simpleRadio">Solutions</label>
                </span>
                <span
                    title="Expanded list of valid solutions to include potential new additions by NYT">
                    <input id="extendedRadio" defaultChecked={boardState.settings.dictionary === "EXTENDED"}
                           type="radio" value="EXTENDED"
                           name="dict"/>
                    <label htmlFor="extendedRadio">NYT Extended</label>
                </span>
                <span
                    title="Only solutions that have not been used yet">
                    <input id="reducedRadio" defaultChecked={boardState.settings.dictionary === "REDUCED"} type="radio"
                           value="REDUCED"
                           name="dict"/>
                    <label htmlFor="reducedRadio">Reduced</label>
                </span>
                <span title="All words that are valid guesses">
                    <input id="bigRadio" defaultChecked={boardState.settings.dictionary === "BIG"} type="radio"
                           value="BIG" name="dict"/>
                    <label htmlFor="bigRadio">All Allowable</label>
                </span>
                <br/>
                International:
                <br/>
                <span
                    title="German Dictionary used by 6mal5.com">
                    <input id="german6mal5Radio" defaultChecked={boardState.settings.dictionary === "GERMAN_6MAL5"}
                           type="radio" value="GERMAN_6MAL5"
                           name="dict"/>
                    <label htmlFor="german6mal5Radio">Deutsch (6mal5.com)</label>
                </span>
                <br/>
                <span
                    title="German Dictionary used by wordle.global/de">
                    <input id="germanGlobalRadio"
                           defaultChecked={boardState.settings.dictionary === "GERMAN_WORDLE_GLOBAL"} type="radio"
                           value="GERMAN_WORDLE_GLOBAL"
                           name="dict"/>
                    <label htmlFor="germanGlobalRadio">Deutsch (wordle.global/de)</label>
                </span>
                <br/>
                <span
                    title="Spanish Dictionary: https://github.com/titoBouzout/Dictionaries/blob/master/Spanish.dic">
                    <input id="spanishRadio" defaultChecked={boardState.settings.dictionary === "SPANISH"} type="radio"
                           value="SPANISH"
                           name="dict"/>
                    <label htmlFor="spanishRadio">Español</label>
                </span>
                <br/>
                <span
                    title="Icelandic Dictionary: https://github.com/titoBouzout/Dictionaries/blob/master/Icelandic.dic">
                    <input id="icelandRadio" defaultChecked={boardState.settings.dictionary === "ICELANDIC"}
                           type="radio" value="ICELANDIC"
                           name="dict"/>
                    <label htmlFor="icelandRadio">Íslensku</label>
                </span>
            </div>
            <hr/>
            <div>
                <div title={hardModeHelpText}>
                    <MDBSwitch id='hardModeSwitch' label="Hard Mode" name="hardMode"
                               defaultChecked={boardState.settings.hardMode} onChange={updateSetting}/>
                </div>
                <div title={rateEnteredWordsHelpText}>
                    <MDBSwitch id='enableWordRatingSwitch' label="Rate Words As You Enter" name="rateEnteredWords"
                               defaultChecked={boardState.settings.rateEnteredWords} onChange={updateSetting}/>
                </div>
                <div title={displayEntropyHelpText}>
                    <MDBSwitch id='displayEntropySwitch' label="Display Entropy" name="displayEntropy"
                               defaultChecked={boardState.settings.displayEntropy} onChange={updateSetting}/>
                </div>
            </div>

            <hr/>

            <h6 title={biasHelpText}>Heuristic Strategy</h6>
            <div onChange={setPreset} className="wordLists">
                Heuristics only:
                <div
                    title={simpleHelpText}>
                    <input id="simpleStratRadio" defaultChecked={boardState.settings.wordConfig === "SIMPLE"}
                           type="radio" value="SIMPLE"
                           name="strat"/>
                    <label htmlFor="simpleStratRadio">Simple</label>
                </div>
                <div
                    title={lowestAverageHelpText}>
                    <input id="optimalMeanRadio" defaultChecked={boardState.settings.wordConfig === "OPTIMAL_MEAN"}
                           type="radio" value="OPTIMAL_MEAN"
                           name="strat"/>
                    <label htmlFor="optimalMeanRadio">Optimal Mean</label>
                </div>
                <hr/>
                With Cut✂ Enabled (may be slower)
                <div
                    title={simplePartitionHelpText}>
                    <input id="simplePartitionStratRadio"
                           defaultChecked={boardState.settings.wordConfig === "SIMPLE_WITH_PARTITIONING"} type="radio"
                           value="SIMPLE_WITH_PARTITIONING"
                           name="strat"/>
                    <label htmlFor="simplePartitionStratRadio">Simple</label>
                </div>
                <div
                    title={lowestAveragePartitionHelpText}>
                    <input id="optimalMeanPartitionRadio"
                           defaultChecked={boardState.settings.wordConfig === "OPTIMAL_MEAN_WITH_PARTITIONING"}
                           type="radio" value="OPTIMAL_MEAN_WITH_PARTITIONING"
                           name="strat"/>
                    <label htmlFor="optimalMeanPartitionRadio">Optimal Mean</label>
                </div>
                <div
                    title={most2HelpText}>
                    <input id="most2Radio" defaultChecked={boardState.settings.wordConfig === "TWO_OR_LESS"}
                           type="radio" value="TWO_OR_LESS"
                           name="strat"/>
                    <label htmlFor="most2Radio">Most Twos</label>
                </div>
                <div
                    title={lowestAverageExtendedPartitionHelpText}>
                    <input id="optimalMeanExtendedPartitionRadio"
                           defaultChecked={boardState.settings.wordConfig === "OPTIMAL_MEAN_EXTENDED_PARTITIONING"}
                           type="radio" value="OPTIMAL_MEAN_EXTENDED_PARTITIONING"
                           name="strat"/>
                    <label htmlFor="optimalMeanExtendedPartitionRadio">Extended Partitioning (experimental -
                        slow)</label>
                </div>
            </div>
        </div>
    );
}

export default Controls;