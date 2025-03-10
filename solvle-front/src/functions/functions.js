export const ALLOWABLE_CHARACTERS = "AÁÄBCDÐEÉFGHIÍJKLMNÑOÓPQRSẞTUÚÜVWXYÝZÞÆÖ"

export function generateRestrictionString(availableLetters, knownLetters, unsureLetters) {
    let restrictionString = "";

    ALLOWABLE_CHARACTERS.split("").filter(letter => availableLetters.has(letter)).forEach(letter => {
        restrictionString += letter;
        knownLetters.forEach((l, pos) => {
            if (l === letter) {
                console.log("Known letter " + letter + " pos " + (pos + 1));
                restrictionString += (pos + 1);
            }
        });
        let hasUnsure = false;
        unsureLetters.forEach((letters, pos) => {
            if (letters.has(letter)) {
                if (!hasUnsure) {
                    hasUnsure = true;
                    restrictionString += "!";
                }
                console.log("unsure letter " + letter + " pos " + (pos + 1));
                restrictionString += (pos + 1);
            }
        });
    })
    return restrictionString;
}

export function generateAnagramString(board) {
    let anagramString = "";

    for(let i = 0; i < board.length ; i++) {
        for(let j = 0; j < board[i].length; j++) {
            if(board[i][j] !== '') {
                anagramString += board[i][j];
            }
        }
    }

    return anagramString;
}

export function generateConfigParams(boardState) {
    let hardMode = boardState.settings.hardMode ?
        "hardMode=true" : "hardMode=false";

    let requireAnswer = boardState.settings.requireAnswer ?
        "&requireAnswer=true" : "&requireAnswer=false";

    let wordConfig = "&wordLength=" + boardState.settings.wordLength + "&wordList=" + boardState.settings.dictionary + "&wordConfig=" + boardState.settings.wordConfig;

    return hardMode + requireAnswer + wordConfig;
}