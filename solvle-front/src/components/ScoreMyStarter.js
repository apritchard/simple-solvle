import React, {useContext, useState} from 'react';
import {Button, Card, Form, ListGroup, Modal, Spinner} from "react-bootstrap";
import AppContext from "../contexts/contexts";
import {generateConfigParams} from "../functions/functions";

function ScoreMyStarter(props) {

    const {
        boardState,
        setSolverOpen
    } = useContext(AppContext);

    const [starter, setStarter] = useState("");
    const [tupleData, setTupleData] = useState({});

    const [modalOpen, setModalOpen] = useState(false);
    const [isLoading, setIsLoading] = useState(false);

    const [submittedStarter, setSubmittedStarter] = useState('');

    const handleShow = () => {
        setModalOpen(true);
        setSolverOpen(true);
    }
    const handleClose = () => {
        setModalOpen(false);
        setSolverOpen(false);
    }

    const changeStarter = (e) => {
        setStarter(e.target.value.replace(/[\s\d]/g, ""));
    };

    const solvePuzzle = (e) => {
        e.preventDefault();
        const pattern = /^\s*[^\s,]{5}(?:\s*,\s*[^\s,]{5})*\s*$/;
        if (!pattern.test(starter)) {
            alert("Please enter one or more 5-character words, separated by commas.");
            return;
        }
        const trimmedStarter = starter.trim();
        setSubmittedStarter(trimmedStarter);
        setIsLoading(true);
        let configParams = generateConfigParams(boardState);

        fetch('/solvle/scoreTuple/' + starter.trim() + "?" + configParams)
            .then(res => res.json())
            .then((data) => {
                console.log("Received guesses:");
                console.log(data);
                setTupleData(data);
            }).catch((error) => {
                console.error('Error fetching solution:', error);
        }).finally(() => {
            setIsLoading(false);
        });
    }

    return (
        <span>
            <Button title="Calculate the entropy and remaining words for your starter word or words" variant="info"
                    onClick={handleShow}>Score Starter</Button>

            <Modal className="solveModal" show={modalOpen} onHide={handleClose} >
                <Modal.Header closeButton>
                    <Modal.Title>Score Starter</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <p>Enter your starting word or word(s), separated by commas. Solvle will calculate how many words this will leave on average and determine the entropy, a measure of how good this guess is at gaining information about the word list you have selected in the options menu.</p>
                    <hr/>
                    <Form onSubmit={solvePuzzle}>
                        <Form.Group className="mb-3" controlId="formSolution">
                            <Form.Label>Starting Word(s)</Form.Label>
                            <Form.Control value={starter} onChange={changeStarter} autoComplete="off" autoFocus
                                          type="text" placeholder="Your starting word(s), comma separated"/>
                        </Form.Group>

                        <Button variant="primary" type="submit">
                            Score My Starter
                        </Button>
                    </Form>

                  <div className="mt-3">
                    {isLoading ? (
                        <div className="text-center">
                            <Spinner animation="border" role="status">
                                <span className="visually-hidden">Loading...</span>
                            </Spinner>
                        </div>
                    ) : (
                        tupleData &&
                        tupleData.partitionStats && (
                            <Card className="mt-4 shadow-sm">
                                <Card.Header as="h5">{submittedStarter} Rating</Card.Header>
                                <ListGroup variant="flush">
                                    <ListGroup.Item>
                                        <strong>Entropy:</strong>{' '}
                                        {Number(tupleData.partitionStats.entropy).toFixed(2)}
                                    </ListGroup.Item>
                                    <ListGroup.Item>
                                        <strong>Words Remaining:</strong>{' '}
                                        {Number(tupleData.partitionStats.wordsRemaining).toFixed(2)}
                                    </ListGroup.Item>
                                </ListGroup>
                            </Card>
                        )
                    )}
                  </div>

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

export default ScoreMyStarter;