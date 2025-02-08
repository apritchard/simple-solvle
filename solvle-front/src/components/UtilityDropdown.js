import React, { useState, useRef } from 'react';
import Dropdown from 'react-bootstrap/Dropdown';

function UtilityDropdown(props) {
    const { children, title = "Utilities" } = props;
    const [show, setShow] = useState(false);
    const timerRef = useRef(null);

    function handleMouseEnter() {
        // Cancel any pending timer to close the dropdown.
        if (timerRef.current) {
            clearTimeout(timerRef.current);
            timerRef.current = null;
        }
        setShow(true);
    }

    function handleMouseLeave() {
        // Close the dropdown after a short delay.
        timerRef.current = setTimeout(function () {
            setShow(false);
        }, 200);
    }

    return (
        <Dropdown as="span" show={show} onMouseEnter={handleMouseEnter} onMouseLeave={handleMouseLeave}>
            <Dropdown.Toggle variant="primary" id="dropdown-utilities">
                {title}
            </Dropdown.Toggle>
            <Dropdown.Menu>
                {React.Children.map(children, function(child, index) {
                    return (
                        <Dropdown.Item as="div" key={index}>
                            {child}
                        </Dropdown.Item>
                    );
                })}
            </Dropdown.Menu>
        </Dropdown>
    );
}

export default UtilityDropdown;