import { useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import UserContext from '../context/UserContext';
import ULink from './MyLink';

function NavComponent() {
    const navigate = useNavigate();

    const { user, setUser } = useContext(UserContext);

    const data = window.localStorage.getItem('user');
    let user1: { email: string } = { email: '' };
    if (data) user1 = JSON.parse(data);

    function handleLogout() {
        setUser({
            loggedIn: false,
            email: '',
        });
        window.localStorage.removeItem('user');
        navigate('sign-in');
    }

    return (
        <div className="relative container mx-auto p-6 shadow-lg mb-5 bg-[#F7F7F7]">
            <div className="flex items-center justify-between">
                {/* logo */}
                {user1.email && (
                    <>
                        <div className="font-bold text-2xl text-center p-2 grow space-x-3">
                            <ULink to="classes">Home</ULink>
                            <ULink to="qr">QR-Code</ULink>
                        </div>
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
