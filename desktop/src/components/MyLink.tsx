import { FunctionComponent } from "react";
import { NavLink } from "react-router-dom";

interface Props {
    children: string | JSX.Element | JSX.Element[] | (() => JSX.Element),
    to: string,
    className?: string,
}
 
const ULink: FunctionComponent<Props> = (props) => {
    return <NavLink 
        to={props.to} 
        className={({isActive, isPending}) => `${props.className ?? ""} ${isActive ? "underline" : ""}`}
        >{props.children}</NavLink>;
}
 
export default ULink;