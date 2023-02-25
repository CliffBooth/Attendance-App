import { useState } from 'react';
import { BrowserRouter } from 'react-router-dom';
import DateComponent from './components/byDate/DateComponent';
import NavComponent from './components/NavComponent';
import Router from './components/Router';
import UserContext from './context/UserContext';

export interface User {
    loggedIn: boolean;
    email: string;
}

function App() {
    const [user, setUser] = useState<User>({
      loggedIn: false, 
      email: ''
    });

    return (
        <BrowserRouter>
            <UserContext.Provider value={{
              user,
              setUser
            }}>
                <NavComponent />
                <Router />
                {/* <DateComponent /> */}
            </UserContext.Provider>
        </BrowserRouter>
    );
}

export default App;
