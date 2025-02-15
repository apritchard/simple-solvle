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

    function handleItemClick(e, child) {
        setShow(false);
        // If the child has its own onClick, call it.
        if (child.props.onClick) {
            child.props.onClick(e);
        }
    }

    return (
        <Dropdown as="span" show={show} onMouseEnter={handleMouseEnter} onMouseLeave={handleMouseLeave}>
            <Dropdown.Toggle variant="primary" id="dropdown-utilities">
                {title}
            </Dropdown.Toggle>
            <Dropdown.Menu>
                {React.Children.map(children, (child, index) => (
                    <Dropdown.Item
                        as="div"
                        key={index}
                        onClick={(e) => handleItemClick(e, child)}
                    >
                        {child}
                    </Dropdown.Item>
                ))}
            </Dropdown.Menu>
        </Dropdown>
    );
}

export default UtilityDropdown;