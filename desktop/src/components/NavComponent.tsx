import { useContext } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import UserContext from '../context/UserContext';

function NavComponent() {
    const navigate = useNavigate();

    const { user, setUser } = useContext(UserContext);

    const data = window.localStorage.getItem('user')
    let user1: {email: string} = {email: ''}
    if (data)
        user1 = JSON.parse(data)


    function handleLogout() {
        setUser({
            loggedIn: false,
            email: '',
        });
        window.localStorage.removeItem('user');
        navigate('sign-in');
    }

    return (
        <div className="relative container mx-auto p-6 shadow-lg mb-5">
            <div className="flex items-center justify-between">
                {/* logo */}
                <div className="font-bold text-2xl text-center p-2">
                    
                </div>
                {user1.email && (
                    <>
                        {/* menu itemes */}
                        <div></div>
                        {/* logout button */}
                        <button
                            onClick={handleLogout}
                            className="bg-gray-300 rounded-md p-2 justify-self-end"
                        >
                            Log out
                        </button>{' '}
                    </>
                )}
            </div>
        </div>
    );
}

export default NavComponent;
