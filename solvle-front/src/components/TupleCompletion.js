import React, {useContext, useEffect, useRef, useState} from 'react';
import {Button, Form, Modal, ProgressBar, Spinner, Table} from "react-bootstrap";
import AppContext from "../contexts/contexts";
import {generateConfigParams} from "../functions/functions";
import {MDBSwitch} from "mdb-react-ui-kit";

function TupleCompletion(props) {
    const {
        boardState,
        setBoardState,
        solverOpen,
        setSolverOpen
    } = useContext(AppContext);

    const [modalOpen, setModalOpen] = useState(false);
    const [sortConfig, setSortConfig] = useState({ key: 'entropy', direction: 'desc' });


    const [firstWord, setFirstWord] = useState("");
    const [suggestions, setSuggestions] = useState([]);
    const [loading, setLoading] = useState(false);
    const [job, setJob] = useState(false);
    const pollingIntervalRef = useRef(null);

    const cancelJob = () => {
        if (pollingIntervalRef.current) {
            clearInterval(pollingIntervalRef.current);
        }
        setLoading(false);
        setJob(null);
        setFirstWord(""); // Clear the input field
    };

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

    const completeTuple = (e) => {
        e.preventDefault();
        // Validate input: each word must be exactly 5 characters long.
        const pattern = /^\s*[^\s,]{5}(?:\s*,\s*[^\s,]{5})*\s*$/;
        if (!pattern.test(firstWord)) {
            alert("Please enter one or more 5-character words, separated by commas.");
            return;
        }
        setLoading(true);
        setJob(null);
        let configParams = generateConfigParams(boardState);
        const url = `solvle/submitTupleJob/${firstWord.trim()}?${configParams}`;

        let intervalId;

        // Define a function to poll the same endpoint
        const pollJob = () => {
            fetch(url)
                .then(res => res.json())
                .then(jobData => {
                    setJob(jobData);
                    if (jobData.status === 'COMPLETED') {
                        setSuggestions(jobData.result);
                        setLoading(false);
                        clearInterval(pollingIntervalRef.current)
                    } else if (jobData.status === 'FAILED') {
                        alert("Job failed: " + jobData.error);
                        setLoading(false);
                        clearInterval(pollingIntervalRef.current)
                    }
                })
                .catch(error => {
                    console.error("Error polling job:", error);
                    setLoading(false);
                    clearInterval(pollingIntervalRef.current)
                });
        };

        // Start polling immediately and then every 2 seconds
        pollJob();
        pollingIntervalRef.current = window.setInterval(pollJob, 2000);
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
                        <div title="Limits multi-word guesses to potential solution words">
                    <MDBSwitch id='requireAnswerSwitch' label="Only Use Potential Answers in Guess" name="requireAnswer"
                               defaultChecked={boardState.settings.requireAnswer} onChange={updateSetting}/>
                </div>
                                                <p><b>Remaining</b>: How many solutions are left on average after guessing all the words in each group (lower is better)</p>
                            <p><b>Entropy</b>: Calculated score based on how well this guess reduces the size of remaining groups (higher is better)</p>

                        {loading && job ? (
                            <Button variant="danger" onClick={cancelJob} type="button">
                                Cancel
                            </Button>
                        ) : (
                            <Button variant="primary" type="submit">
                                Solve!
                            </Button>
                        )}
                    </Form>
                    <hr/>
                    {/* If loading and we have a job, show a progress bar */}
                    {loading && job && job.tasks > 0 ? (
                        <div className="job-progress-container text-center my-3">
                            <h5>Job Progress</h5>
                            <ProgressBar
                                animated
                                // Change the color based on progress: warning for low, info for mid, success for high
                                variant={
                                    (job.completedTasks / job.tasks) >= 0.75
                                        ? 'success'
                                        : (job.completedTasks / job.tasks) >= 0.5
                                            ? 'info'
                                            : 'warning'
                                }
                                now={(job.completedTasks / job.tasks) * 100}
                                label={`${((job.completedTasks / job.tasks) * 100).toFixed(0)}%`}
                                style={{height: '30px', fontWeight: 'bold', fontSize: '1.1rem'}}
                            />
                            <p className="mt-2">
                                Evaluated {job.completedTasks} out of {job.tasks} combinations.
                            </p>
                        </div>
                    ) : loading && (
                        // Fallback spinner if no job info is yet available
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