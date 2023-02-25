import { useNavigate } from "react-router-dom"
import { Class } from "../../services/ApiService"

interface Props {
    subject_name: string,
    classes?: Class[]
}

const SubjectComponent: React.FC<Props> = ({
    subject_name,
    classes
}) => {

    const navigate = useNavigate()

    function handleClick() {
        navigate('/list', { state: { classes } })
    }

    return (
        //make this link to display a table of attendance, pass classes as a prop
        <div className="p-5 text-3xl font-bold border-2 shadow
        hover:scale-110 transition-all duration-150 cursor-pointer"
            onClick={handleClick}
        >
            {subject_name}
        </div>
    )
}

export default SubjectComponent