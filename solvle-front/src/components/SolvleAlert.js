import React, {useState} from 'react';
import {Alert, Button} from "react-bootstrap";

function SolvleAlert({heading, message, persist, persistMessage, persistVariant}) {
    const [show, setShow] = useState(localStorage.getItem("helpSeen2") !== 'true');

    if (show) {
        return (
            <div>
            <Alert className="alertDialog" variant="info" onClose={() => {
                localStorage.setItem("helpSeen2", 'true');
                setShow(false);
            }} dismissible>
                <Alert.Heading>{heading} </Alert.Heading>
                <div className="instructions">
                    {message}
                </div>
            </Alert>
            </div>
        );
    }
    if (persist) {
        return <Button className="alertButton" variant={persistVariant} onClick={() => {
            localStorage.setItem("helpSeen2", 'false');
            setShow(true);
        }}>{persistMessage}</Button>;
    }
}

export default SolvleAlert;