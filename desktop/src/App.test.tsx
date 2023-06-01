import { fireEvent, render, screen } from '@testing-library/react';
import Home from './layouts/Home';
import App from './App'
import UserContext from './context/UserContext';

const user = {
    email: 'test',
    password: 'test'
}

beforeAll(() => {
    localStorage.setItem('user', JSON.stringify(user))
})

afterAll(() => {
    localStorage.removeItem('user')
})

test('test display', async () => {
    const user = {
        loggedIn: true,
        email: 'test',
    }
    render(
        <UserContext.Provider value={{
            user,
            setUser: ()=>{}
        }
        }>
            <App />
        </UserContext.Provider>
    )
    const text1 = screen.getByText('previous classes:')
    const text2 = screen.getByText('Your predefined classes:')
    expect(text1).toBeInTheDocument();
    expect(text2).toBeInTheDocument();
    
    fireEvent.click(screen.getByText("Start class"), {button: 0}) 

    const text3 = screen.getByText('Class name:')
    expect(text3).toBeInTheDocument();
})