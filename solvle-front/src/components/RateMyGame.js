import React, {useContext, useState} from 'react';
import {Button, Col, Form, Modal, Row, Spinner} from "react-bootstrap";
import AppContext from "../contexts/contexts";
import {ALLOWABLE_CHARACTERS, generateConfigParams} from "../functions/functions";


function RateMyGame(props) {

    const {
        setSolverOpen,
        boardState,
        setBoardState,
    } = useContext(AppContext);

    const [solution, setSolution] = useState(boardState.settings.autoColorWord.toUpperCase());
    const [board, setBoard] = useState(boardState.board);
    const [modalOpen, setModalOpen] = useState(false);
    const [rateData, setRateData] = useState();
    const [isLoading, setIsLoading] = useState(false);
    const [showCopiedMessage, setShowCopiedMessage] = useState(false);

    const clipboardAvailable = navigator.clipboard !== null && navigator.clipboard !== undefined;

    const handleShow = (e) => {
        if (e && e.target) {
            e.target.blur();
        }
        setModalOpen(true);
        setSolverOpen(true);
        setSolution(boardState.settings.autoColorWord.toUpperCase());
        if(board !== boardState.board) {
            setRateData(null);
            setBoard(boardState.board);
        }
    }
    const handleClose = (e) => {
        if(e != undefined) {
            e.preventDefault();
        }
        setModalOpen(false);
        setSolverOpen(false);
    }

    const changeSolution = (e) => {
        setSolution(e.target.value.toUpperCase()
            .split('')
            .filter(char =>ALLOWABLE_CHARACTERS.includes(char))
            .slice(0, boardState.settings.wordLength)
            .join(''));
    }

    const changeRow = (e, rowIndex) => {
        // Convert string back to array of characters
        const updatedRow = e.target.value
            .toUpperCase()
            .split('')
            .filter(char => ALLOWABLE_CHARACTERS.includes(char))
            .slice(0, boardState.settings.wordLength);
        const newRows = [...board];
        newRows[rowIndex] = updatedRow;
        setBoard(newRows);
        console.log("Setting board to", newRows);
    };

    const rateGame = (e) => {
        e.preventDefault();
        setIsLoading(true);
        let configParams = generateConfigParams(boardState);
        const guesses = board.map(row => row.join('')).filter(row => row.length == boardState.settings.wordLength).map(guess => 'guesses=' + encodeURIComponent(guess)).join('&');
        console.log(guesses);

        fetch('/solvle/rate/' + solution.trim() + "?" + guesses + "&" + configParams)
            .then(res => res.json())
            .then((data) => {
                if(!data?.rows){
                    console.log("No game data received")
                    return;
                }
                console.log("Received data:");
                console.log(data);
                setRateData(data);
            }).catch(error => {
                console.log("Bad request for game data.");
            }).finally(() => {setIsLoading(false)});
    }

    function calculateRating(rateData) {
        if(!rateData || !rateData.luck) {
            return "";
        }
        let lucky = rateData.luck > .60;
        let strategic = rateData.skill > .60;
        let fishy = rateData.heuristic > .60;

        if(lucky) {
            if(strategic && !fishy) {
                return "Aspect of Owl 🦉"
            } else if (strategic && fishy) {
                return "Aspect of Fox 🦊"
            } else if (!strategic && fishy) {
                return "Aspect of Whale 🐋"
            } else if (!strategic && !fishy) {
                return "Aspect of Rhino 🦏"
            }
        } else {
            if(strategic && !fishy) {
                return "Aspect of Horse 🐴"
            } else if (strategic && fishy) {
                return "Aspect of Chameleon 🦎"
            } else if (!strategic && fishy) {
                return "Aspect of Salmon 🐟"
            } else if (!strategic && !fishy) {
                return "Aspect of Turtle 🐢"
            }
        }
    }

    function copyGameDataToClipboard(hideSpoilers = false) {
        const colorizeWord = (guess, finalWord) => {
            const result = [];
            const finalWordLetters = finalWord.split('');

            const letterCounts = finalWordLetters.reduce((acc, letter) => {
                acc[letter] = (acc[letter] || 0) + 1;
                return acc;
            }, {});

            // Loop over each letter in guess to determine color
            guess.forEach((letter, index) => {
                if (letter === finalWordLetters[index]) {
                    result.push('🟩'); // Correct position
                    letterCounts[letter]--; // Reduce count of this letter
                } else if (letterCounts[letter] > 0) {
                    result.push('🟨'); // Correct letter, wrong position
                    letterCounts[letter]--; // Reduce count to prevent over marking
                } else {
                    result.push('⬜'); // Letter not in finalWord at all
                }
            });
            return result.join('') + (hideSpoilers ? '' : ' ' + guess.join('').toUpperCase());
        };

        let clipboardText = board.map((row, index) => {
            if(index >= rateData.rows.length) {
                return;
            }
            const finalWord = rateData.rows[rateData.rows.length-1].playerWord.toUpperCase();
            const coloredGuess = colorizeWord(row, finalWord);
            const remaining = rateData?.rows[index]?.actualRemaining.toString().padStart(4, ' ');
            const skill = Math.round(rateData?.rows[index]?.skill * 100).toString().padStart(3, ' ');
            const luck = Math.round(rateData?.rows[index]?.luck * 100).toString().padStart(3, ' ');
            const fish = Math.round(rateData?.rows[index]?.heuristic * 100).toString().padStart(3, ' ');
            return `${coloredGuess} (${remaining}) L:${luck} S✂:${skill} S🐟:${fish}`;
        }).join('  \n');



        clipboardText += `  \nLuck: ${Math.round(rateData.luck * 100)} Skill✂: ${Math.round(rateData.skill * 100)} Skill🐟: ${Math.round(rateData.heuristic * 100)}  \n`;
        clipboardText += calculateRating(rateData);
        clipboardText += "  \nhttps://solvle.appsoil.com";

        navigator.clipboard.writeText(clipboardText).then(() => {
            console.log(clipboardText);
            setShowCopiedMessage(true);
            setTimeout(() => setShowCopiedMessage(false), 2000);
        }).catch(err => {
            console.error('Failed to copy data:', err);
        });
    }


    let buttonText = "Rate My Game";

    return (
        <span>
            <Button title="Rate Performance of a Completed Game" variant="primary"
                    onClick={handleShow}>{buttonText}</Button>

            <Modal className="rateMyGameModal" size="lg" show={modalOpen} onHide={handleClose} >
                <Modal.Header closeButton>
                    <Modal.Title>Rate My Game</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Form onSubmit={rateGame} className="flex-form">
                        <Form.Group className="mb-3" controlId="formSolution">
                            <Form.Label>Solution</Form.Label>
                            <Form.Control value={solution} onChange={changeSolution} autoComplete="off" autoFocus
                                          type="text" placeholder="The answer to today's puzzle goes here"/>
                        </Form.Group>
                        <Row className="narrow-row rate-game-header">
                            <Col md={3} s={3} xs={3}><strong>Guess</strong></Col>
                            <Col><strong>Expected</strong></Col>
                            <Col><strong>Actual</strong></Col>
                            <Col><strong>Luck</strong></Col>
                            <Col><strong>✂Skill</strong></Col>
                            <Col><strong>🐟Skill</strong></Col>
                            <Col><strong>Suggestion</strong></Col>
                        </Row>
                        {board.map((row, index) => (
                            <Row key={"row" + index} className="mb-3 narrow-row">
                                <Col md={3} s={3} xs={3}>
                                    <Form.Control
                                        type="text"
                                        placeholder={"Guess " + (index + 1)}
                                        value={row.join('')}
                                        onChange={e => changeRow(e, index)}
                                    />
                                </Col>
                                <Col>{Math.round(rateData?.rows[index]?.playerScore.remainingWords) || "-"}</Col>
                                <Col>{rateData?.rows[index]?.actualRemaining}</Col>
                                <Col className={!isLoading ? 'fade-in' : ''}>{Math.round(rateData?.rows[index]?.luck * 100) || "-"}</Col>
                                <Col className={!isLoading ? 'fade-in' : ''}>{Math.round(rateData?.rows[index]?.skill * 100) || "-"}</Col>
                                <Col className={!isLoading ? 'fade-in' : ''}>{Math.round(rateData?.rows[index]?.heuristic * 100) || "-"}</Col>
                                <Col className={!isLoading ? 'fade-in' : ''}>{rateData?.rows[index]?.solvleWord || "-"}</Col>
                            </Row>
                        ))}
                        <Row className={!isLoading ? 'fade-in' : ''}>
                            <Col md={3} s={3} xs={3}><strong>Overall:</strong></Col>
                            <Col md={3} s={3} xs={3}>{calculateRating(rateData)}</Col>
                            <Col>{Math.round(rateData?.luck * 100) || "-"}</Col>
                            <Col>{Math.round(rateData?.skill * 100) || "-"}</Col>
                            <Col>{Math.round(rateData?.heuristic * 100) || "-"}</Col>
                            <Col></Col>
                        </Row>
                        <div className="rate-buttons">
                            {isLoading ? (<Spinner animation="border" role="status"><span
                                className="visually-hidden">Loading...</span></Spinner>) :
                                (<div className="spaced-buttons"><Button variant="primary" type="submit" disabled={isLoading}>
                                    Rate!
                                    </Button>
                                        {clipboardAvailable && rateData?.luck && <Button variant="info" onClick={() => copyGameDataToClipboard(false)}>Copy</Button>}
                                        {clipboardAvailable && rateData?.luck && <Button variant="info" onClick={() => copyGameDataToClipboard(true)}>Copy (spoiler-free)</Button>}
                                        {showCopiedMessage && (
                                            <span className="fade-out">Score copied</span>
                                        )}
                                </div>
                                )
                            }
                        </div>
                    </Form>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={handleClose}>
                        Close
                    </Button>
                </Modal.Footer>
            </Modal>
        </span>
    );
}

export default RateMyGame;