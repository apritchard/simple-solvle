import React, {useContext, useEffect, useState} from 'react';
import {Button, Form, Modal, Spinner, Table} from "react-bootstrap";
import AppContext from "../contexts/contexts";
import {generateConfigParams} from "../functions/functions";

function TupleCompletion(props) {
    const {
        boardState,
        solverOpen,
        setSolverOpen
    } = useContext(AppContext);

    const [modalOpen, setModalOpen] = useState(false);
    const [sortConfig, setSortConfig] = useState({ key: 'entropy', direction: 'desc' });


    const [firstWord, setFirstWord] = useState("");
    const [suggestions, setSuggestions] = useState([]);
    const [loading, setLoading] = useState(false);

    const handleShow = () => {
        setModalOpen(true);
        setSolverOpen(true);
    }
    const handleClose = () => {
        setModalOpen(false);
        setSolverOpen(false);
    }
    const changeFirstWord = (e) => {
        setFirstWord((e.target.value).replace(/\s/g, ""));
    }

    const completeTuple = (e) => {
        e.preventDefault();
        // Validate input: each word must be exactly 5 characters long.
        const pattern = /^\s*[^\s,]{5}(?:\s*,\s*[^\s,]{5})*\s*$/;
        if (!pattern.test(firstWord)) {
            alert("Please enter one or more 5-character words, separated by commas.");
            return;
        }
        setLoading(true);
        let configParams = generateConfigParams(boardState);

        fetch('/solvle/finishTuple/' + firstWord.trim() + "?" + configParams)
            .then(res => res.json())
            .then((data) => {
                console.log("Received suggestions:");
                console.log(data);
                setSuggestions(data);
                setLoading(false)
            })
            .catch(error => {
            console.error("Error fetching suggestions:", error);
            setLoading(false);
        });
    }

    // Handle header clicks for sorting.
    const handleSort = (key) => {
        if (sortConfig.key === key) {
            // Toggle sort direction when the same header is clicked.
            setSortConfig({ key, direction: sortConfig.direction === 'asc' ? 'desc' : 'asc' });
        } else {
            // Set default sort direction for each key:
            let defaultDirection = 'asc';
            if (key === 'entropy') {
                defaultDirection = 'desc';
            }
            setSortConfig({ key, direction: defaultDirection });
        }
    };

    // Create a sorted copy of suggestions according to the current sort configuration.
    const sortedSuggestions = [...suggestions];
    if (sortConfig.key !== null) {
        sortedSuggestions.sort((a, b) => {
            let aVal, bVal;
            switch (sortConfig.key) {
                case 'tuple': {
                    // We assume that the user-provided word has order === 0.
                    // So the suggestion is the other word.
                    const aSuggestion = a.tuple.find(t => t.order !== 0) || a.tuple[1];
                    const bSuggestion = b.tuple.find(t => t.order !== 0) || b.tuple[1];
                    aVal = aSuggestion.word.toLowerCase();
                    bVal = bSuggestion.word.toLowerCase();
                    return sortConfig.direction === 'asc'
                        ? aVal.localeCompare(bVal)
                        : bVal.localeCompare(aVal);
                }
                case 'wordsRemaining':
                    aVal = a.partitionStats.wordsRemaining;
                    bVal = b.partitionStats.wordsRemaining;
                    break;
                case 'entropy':
                    aVal = a.partitionStats.entropy;
                    bVal = b.partitionStats.entropy;
                    break;
                default:
                    aVal = 0;
                    bVal = 0;
            }
            if (aVal < bVal) return sortConfig.direction === 'asc' ? -1 : 1;
            if (aVal > bVal) return sortConfig.direction === 'asc' ? 1 : -1;
            return 0;
        });
    }


    return (
        <span>
            <Button title="Find multi-word openers!" variant="info"
                    onClick={handleShow}>Multi-word Starters</Button>
            <Modal className="tupleCompletion" show={modalOpen} onHide={handleClose} >
                <Modal.Header closeButton>
                    <Modal.Title>Multi-word Starters</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <p>Enter your first word and get suggestions on the best all-purpose second word to use with it.</p>
                    <Form onSubmit={completeTuple}>
                        <Form.Group className="mb-3" controlId="formStartingWords">
                            <Form.Control value={firstWord} onChange={changeFirstWord} autoComplete="off"
                                          type="text" placeholder="Starting Word(s), separated by commas"/>
                        </Form.Group>
                                                <p><b>Remaining</b>: How many solutions are left on average after guessing all the words in each group (lower is better)</p>
                            <p><b>Entropy</b>: Calculated score based on how well this guess reduces the size of remaining groups (higher is better)</p>
                        <Button variant="primary" type="submit">
                            Solve!
                        </Button>
                    </Form>
                    <hr/>
                    {/* Show spinner while loading */}
                    {loading && (
                        <div className="text-center">
                            <Spinner animation="border" role="status">
                                <span className="visually-hidden">Loading...</span>
                            </Spinner>
                        </div>
                    )}
                    {/* Display results if available and not loading */}
                    {!loading && suggestions.length > 0 && (
                        <div style={{maxHeight: '300px', overflowY: 'scroll'}}>
                            <Table striped bordered hover size="sm">
                                <thead>
                                <tr>
                                    <th
                                        onClick={() => handleSort('tuple')}
                                        style={{cursor: 'pointer'}}
                                    >
                                        Tuple {sortConfig.key === 'tuple' ? (sortConfig.direction === 'asc' ? '▲' : '▼') : ''}
                                    </th>
                                    <th
                                        onClick={() => handleSort('wordsRemaining')}
                                        style={{cursor: 'pointer'}}
                                    >
                                        Remaining {sortConfig.key === 'wordsRemaining' ? (sortConfig.direction === 'asc' ? '▲' : '▼') : ''}
                                    </th>
                                    <th
                                        onClick={() => handleSort('entropy')}
                                        style={{cursor: 'pointer'}}
                                    >
                                        Entropy {sortConfig.key === 'entropy' ? (sortConfig.direction === 'asc' ? '▲' : '▼') : ''}
                                    </th>
                                </tr>
                                </thead>
                                <tbody>
                                {sortedSuggestions.map((suggestion, idx) => {
                                    // Sort the tuple by order and join all words
                                    const tupleDisplay = '(' + suggestion.tuple
                                        .slice()
                                        .sort((a, b) => a.order - b.order)
                                        .map(t => t.word)
                                        .join(', ') + ')';
                                    return (
                                        <tr key={idx}>
                                            <td>{tupleDisplay}</td>
                                            <td>{Number(suggestion.partitionStats.wordsRemaining).toFixed(2)}</td>
                                            <td>{Number(suggestion.partitionStats.entropy).toFixed(2)}</td>
                                        </tr>
                                    );
                                })}
                                </tbody>
                            </Table>
                        </div>
                    )}
                </Modal.Body>
            </Modal>
        </span>
    );
}

export default TupleCompletion;