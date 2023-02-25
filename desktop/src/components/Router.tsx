import { useContext, useEffect } from "react"
import { Route, Routes, useNavigate } from "react-router-dom"
import UserContext from "../context/UserContext"
import AttendanceView from "./bySubject/AttendanceView"
import ClassesScreen from "./ClassesScreen"
import Home from "./Home"
import LoginPage from "./LoginPage"

const Router = () => {

    const { setUser } = useContext(UserContext)

    const navigate = useNavigate()

    console.log('router rendered!')

    useEffect(() => {
        const data = window.localStorage.getItem('user')
        if (!data) {
            navigate('/sign-in')
        } else {
            const user = JSON.parse(data)
            setUser({
                loggedIn: true,
                email: user.email
            })
        }
        console.log('user is set!')
    }, [])
    
    return (
            <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/sign-in" element={<LoginPage />} />
                <Route path="/classes" element={<ClassesScreen />} />
                <Route path='/list' element={<AttendanceView />} />
            </Routes>
    )
}

export default Router