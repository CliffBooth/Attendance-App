import { createContext, Dispatch, SetStateAction } from 'react';
import { User } from '../App'

export interface LoggedInContext {
    user: User
    setUser: Dispatch<SetStateAction<User>>;
}

const UserContext = createContext<LoggedInContext>({
    user: {
        loggedIn: false,
        email: ''
    },
    setUser: () => {}
});

export default UserContext