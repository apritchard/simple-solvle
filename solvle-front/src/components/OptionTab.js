import React, {useContext} from 'react';
import AppContext from "../contexts/contexts";

function OptionTab({wordList, onSelectWord, heading, solutionList}) {
    const {
        boardState,
    } = useContext(AppContext);
    return (
        <div>
            <div>{heading}</div>
            <ol>
                {[...wordList].slice(0, 100).map((item, index) => (
                    <li className={`optionItem ${item.partitionStats?.ruts && item.partitionStats.ruts.length > 0 ? 'rutDetected' : ''}`}
                        key={item.word}
                        value={index + 1}
                        title={item.partitionStats?.ruts && item.partitionStats.ruts.length > 0 ? `Potential Ruts Detected: ${item.partitionStats.ruts.join(', ')}` : ''}
                        onClick={() => onSelectWord(item.word.toUpperCase())}>
                        {solutionList?.some(x => x.word === item.word) ?
                            <strong>{item.word}</strong> :
                            item.word
                        }
                        {" ("}
                        {item.partitionStats?.wordsRemaining > 0 ?
                            boardState.settings.displayEntropy ?
                                item.partitionStats.entropy.toFixed(2) :
                                item.partitionStats.wordsRemaining.toFixed(1)
                            :
                            (item.freqScore * 100).toFixed(0) + "%"
                        }
                        {")"}
                    </li>
                ))}

            </ol>
        </div>
    );
}

export default OptionTab;