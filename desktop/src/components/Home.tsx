import { useContext } from "react";
import { Navigate } from "react-router-dom";
import UserContext from "../context/UserContext";

const Home = () => {

    console.log('home render!')

    // const { user } = useContext(UserContext)
    const data = window.localStorage.getItem('user')
    let user: {email: string} = {email: ''}
    if (data)
        user = JSON.parse(data)

    return (
        <div>
            Home
            {user.email && <Navigate to={'classes'}/>}
        </div>
    );
};

export default Home;