import React, {useContext} from 'react';
import AppContext from "../contexts/contexts";

function RowScore({rowNumber}) {
    const {
        rowScores
    } = useContext(AppContext);

    let fishing = rowScores[rowNumber] && rowScores[rowNumber].fishingScore ? (rowScores[rowNumber].fishingScore * 100).toFixed(0) + "%" : "";
    let remaining  = rowScores[rowNumber] && rowScores[rowNumber].remainingWords ? rowScores[rowNumber].remainingWords.toFixed(1) : "";
    let entropy = rowScores[rowNumber] && rowScores[rowNumber].entropy ? rowScores[rowNumber].entropy.toFixed(2) : "";
    console.log("Row score ", rowScores[rowNumber]);

    return (
        <div className="rowScore" title={"This word scored " + fishing +
            " as a fishing word using the current configuration and would reduce the remaining words to an average of " + remaining +
            ". It is rated as an Entropy increase of " + entropy + ", based on the average size of the resultant groupings."}>
            {rowNumber === 0 && <div>🐟✂</div> }
            <div>{fishing}</div>
            <div>{remaining}</div>
            <div>{entropy}</div>
        </div>
    );
}

export default RowScore;