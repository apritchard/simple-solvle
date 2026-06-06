import React, {useContext, useEffect, useMemo, useState} from 'react';
import AppContext from "../contexts/contexts";
import OptionTab from "./OptionTab";
import {Spinner, Tab, Tabs} from "react-bootstrap";
import {generateConfigParams, generateRestrictionString} from "../functions/functions";

function Options(props) {

    const {
        currentOptions,
        setCurrentOptions,
        tileColors,
        onSelectWord,
        boardState,
    } = useContext(AppContext);

    const [loading, setLoading] = useState(true);
    const {
        dictionary,
        hardMode,
        requireAnswer,
        usePartitioning,
        wordConfig,
        wordLength
    } = boardState.settings;
    const shouldUpdate = boardState.shouldUpdate;

    // Restriction string depends only on colored tiles, so this stays a stable
    // primitive while the user types uncolored letters and only changes when the
    // board coloring does, avoiding refetches on every keystroke.
    const restrictionString = useMemo(
        () => generateRestrictionString(boardState.board, tileColors),
        [boardState.board, tileColors]);

    useEffect(() => {
        setLoading(true);

        console.log("Fetching " + restrictionString + " dictionary:" + dictionary + " partitioning:" + usePartitioning);

        let configParams = generateConfigParams({
            settings: {
                dictionary,
                hardMode,
                requireAnswer,
                wordConfig,
                wordLength
            }
        });

        // encodeURIComponent so the ^ (min) and $ (max) frequency tokens survive
        // the path; Tomcat rejects a raw ^ with a 400.
        fetch('/solvle/' + encodeURIComponent(restrictionString) + "?" + configParams)
            .then(res => {
                if (res.ok) {
                    return res.json()
                }
                throw new Error(res.statusMessage);
            })
            .then((data) => {
                console.log("options received:")
                console.log(data);
                setCurrentOptions(data);
                setLoading(false);
            }).catch((e) => {
                console.log("Error retrieving options for " + restrictionString);
                setCurrentOptions({
                    wordList: [{naturalOrdering: 1, word: 'An Error Has Occurred', freqScore: 0.00}],
                    fishingWords: [{naturalOrdering: 1, word: 'An Error Has Occurred', freqScore: 0.00}],
                    bestWords: [{naturalOrdering: 1, word: 'An Error Has Occurred', freqScore: 0.00}],
                    wordsWithCharacter: new Map(),
                    totalWords: 0,
                    knownPositions: new Set()
                });
                setLoading(false);
        });
    }, [setCurrentOptions, restrictionString, dictionary, usePartitioning, shouldUpdate,
        hardMode, requireAnswer, wordConfig, wordLength]);

    return (

        <div className="options">
            <Tabs id="possible-word-tabs" className="flex-nowrap tabList">
                <Tab eventKey="viable" title="Pick👍" tabClassName="viableTab" tabAttrs={{title:"Words suggested based on how common their characters are among all the possible words. Click a word to add it to the board."}}>
                    {loading && <div>Loading...<Spinner animation="border" role="status" /> </div>}
                    {!loading && <OptionTab wordList={currentOptions.wordList} onSelectWord={onSelectWord}
                               heading={currentOptions.totalWords + " possible words"}/> }
                </Tab>
                <Tab eventKey="fishing" title="Fish🐟" tabClassName="fishingTab" tabAttrs={{title:"Words that maximize revealing new letters based on their frequency in the viable word set. Includes non-viable solutions."}}>
                    {loading && <div>Loading...<Spinner animation="border" role="status" /> </div>}
                    {!loading && <OptionTab wordList={currentOptions.fishingWords} onSelectWord={onSelectWord} solutionList={currentOptions.wordList}
                               heading={"Fishing Words"}/> }
                </Tab>
                { usePartitioning && currentOptions.bestWords !== null &&
                    <Tab eventKey="Remain" title="Cut✂" tabClassName="remTab" tabAttrs={{title:"Words that leave the fewest remaining choices."}}>
                    {loading && <div>Loading...<Spinner animation="border" role="status" /> </div>}
                    {!loading && <OptionTab wordList={currentOptions.bestWords} onSelectWord={onSelectWord} solutionList={currentOptions.wordList}
                               heading={currentOptions.bestWords.length <= 0 ? "Too many viable words " : "Optimize Entropy"}/> }
                </Tab> }

                { (!usePartitioning || currentOptions.bestWords === null) &&
                    <Tab eventKey="Remain" title="Cut✂" tabClassName="remTab">
                        <div>Select a different heuristic strategy in the options menu to enable.</div>
                    </Tab>
                }

            </Tabs>
        </div>

    );
}

export default Options;
