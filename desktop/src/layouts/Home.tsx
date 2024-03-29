import { useContext } from "react";
import { Navigate } from "react-router-dom";
import UserContext from "../context/UserContext";
import ClassesScreen from "./ClassesScreen";
import CreatedClasses from "./CreatedClasses";
import { useTranslation } from "react-i18next";

const Home = () => {

    console.log('home render!')

    // const { user } = useContext(UserContext)
    const data = window.localStorage.getItem('user')
    let user: {email: string} = {email: ''}
    if (data)
        user = JSON.parse(data)

    const {t} = useTranslation();

    console.log('user = ', user)
    return (
        <div className="container mx-auto p-6">
        {user.email &&
            <>
                <p className="mb-5"><>{t('previous classes')}</>:</p>
                <ClassesScreen />
                <p className="mt-10 mb-5"><>{t('Your predefined classes')}</>:</p>
                <CreatedClasses />
            </>
        }
        </div>
    );
};

export default Home;