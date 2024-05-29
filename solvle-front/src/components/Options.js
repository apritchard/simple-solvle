import React, {useContext, useEffect, useState} from 'react';
import AppContext from "../contexts/contexts";
import OptionTab from "./OptionTab";
import {Spinner, Tab, Tabs} from "react-bootstrap";
import {generateConfigParams, generateRestrictionString} from "../functions/functions";

function Options(props) {

    const {
        currentOptions,
        setCurrentOptions,
        availableLetters,
        knownLetters,
        unsureLetters,
        onSelectWord,
        boardState,
    } = useContext(AppContext);

    const [loading, setLoading] = useState(true);

    useEffect(() => {
        setLoading(true);

        console.log("Available letters: " + [...availableLetters]);

        let restrictionString = generateRestrictionString(availableLetters, knownLetters, unsureLetters);

        console.log("Fetching " + restrictionString + " dictionary:" + boardState.settings.dictionary + " partitioning:" + boardState.settings.usePartitioning);

        let configParams = generateConfigParams(boardState);

        fetch('/solvle/' + restrictionString + "?" + configParams)
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
    }, [setCurrentOptions, boardState.settings.wordLength, boardState.settings.dictionary, boardState.settings.usePartitioning, boardState.shouldUpdate,
        availableLetters, knownLetters, unsureLetters]);

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
                    {!loading && <OptionTab wordList={currentOptions.fishingWords} onSelectWord={onSelectWord}
                               heading={"Fishing Words"}/> }
                </Tab>
                { boardState.settings.usePartitioning && currentOptions.bestWords !== null &&
                    <Tab eventKey="Remain" title="Cut✂" tabClassName="remTab" tabAttrs={{title:"Words that leave the fewest remaining choices."}}>
                    {loading && <div>Loading...<Spinner animation="border" role="status" /> </div>}
                    {!loading && <OptionTab wordList={currentOptions.bestWords} onSelectWord={onSelectWord}
                               heading={currentOptions.bestWords.length <= 0 ? "Too many viable words " : "Minimize Remaining"}/> }
                </Tab> }

                { (!boardState.settings.usePartitioning || currentOptions.bestWords === null) &&
                    <Tab eventKey="Remain" title="Cut✂" tabClassName="remTab">
                        <div>Select a different heuristic strategy in the options menu to enable.</div>
                    </Tab>
                }

            </Tabs>
        </div>

    );
}

export default Options;