import React, {useContext, useEffect, useState} from 'react';
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
                return "Use Math"
            } else if (strategic && fishy) {
                return "Smart AND Lucky?"
            } else if (!strategic && fishy) {
                return "Makes sense"
            } else if (!strategic && !fishy) {
                return "That's one way to do it"
            }
        } else {
            if(strategic && !fishy) {
                return "Better luck next time"
            } else if (strategic && fishy) {
                return "Thems the breaks"
            } else if (!strategic && fishy) {
                return "Could happen to anyone"
            } else if (!strategic && !fishy) {
                return "Ouch"
            }
        }
    }

    let buttonText = "Rate My Game";

    return (
        <span>
            <Button title="Rate Performance of a Completed Game" variant="primary"
                    onClick={handleShow}>{buttonText}</Button>

            <Modal className="rateMyGameModal" show={modalOpen} onHide={handleClose} >
                <Modal.Header closeButton>
                    <Modal.Title>Rate My Game</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Form onSubmit={rateGame}>
                        <Form.Group className="mb-3" controlId="formSolution">
                            <Form.Label>Solution</Form.Label>
                            <Form.Control value={solution} onChange={changeSolution} autoComplete="off" autoFocus
                                          type="text" placeholder="The answer to today's puzzle goes here"/>
                        </Form.Group>
                        <Row>
                            <Col md={3}><strong>Guess</strong></Col>
                            <Col><strong>Luck</strong></Col>
                            <Col><strong>Skill ✂</strong></Col>
                            <Col><strong>Skill 🐟</strong></Col>
                            <Col><strong>Suggestion</strong></Col>
                        </Row>
                        {board.map((row, index) => (
                            <Row key={"row" + index} className="mb-3">
                                <Col md={3}>
                                    <Form.Control
                                        type="text"
                                        placeholder={"Guess " + (index + 1)}
                                        value={row.join('')}
                                        onChange={e => changeRow(e, index)}
                                    />
                                </Col>
                                <Col className={!isLoading ? 'fade-in' : ''}>{Math.round(rateData?.rows[index]?.luck * 100) || "-"}</Col>
                                <Col className={!isLoading ? 'fade-in' : ''}>{Math.round(rateData?.rows[index]?.skill * 100) || "-"}</Col>
                                <Col className={!isLoading ? 'fade-in' : ''}>{Math.round(rateData?.rows[index]?.heuristic * 100) || "-"}</Col>
                                <Col className={!isLoading ? 'fade-in' : ''}>{rateData?.rows[index]?.solvleWord || "-"}</Col>
                            </Row>
                        ))}
                        <Row className={!isLoading ? 'fade-in' : ''}>
                            <Col md={3}><strong>Overall:</strong></Col>
                            <Col>{Math.round(rateData?.skill * 100)}</Col>
                            <Col>{Math.round(rateData?.luck * 100)}</Col>
                            <Col>{Math.round(rateData?.heuristic * 100)}</Col>
                            <Col></Col>
                        </Row>
                        {isLoading ? (<Spinner animation="border" role="status"><span
                            className="visually-hidden">Loading...</span></Spinner>) :
                            (<div><Button variant="primary" type="submit" disabled={isLoading}>
                                Rate!
                                </Button><span className="ms-3">{calculateRating(rateData)}</span></div>
                            )
                        }
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