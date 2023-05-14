import { useNavigate } from "react-router-dom"
import { Class, getPredefinedClasses } from "../../services/ApiService"

interface Props {
    subject_name: string,
    classes?: Class[]
}

const SubjectComponent: React.FC<Props> = ({
    subject_name,
    classes //list of classes with subject_name
}) => {

    const navigate = useNavigate()

    async function handleClick() {
        const resp = await getPredefinedClasses()
        if (resp.status === 'success' && resp.data) {
            const predefined = resp.data.find(p => p.subjectName.toLowerCase() === subject_name.toLowerCase())
            navigate('/list', {state: {classes, predefined}})
        } else {
            console.log('cannot fetch predefined clases!! ', resp.message)
            navigate('/list', { state: { classes } })
        }
        
    }

    return (
        //make this link to display a table of attendance, pass classes as a prop
        <div className="p-5 text-3xl font-bold border-2 shadow
        hover:scale-110 transition-all duration-150 cursor-pointer"
            onClick={handleClick}
        >
            {subject_name}
            <p className="font-normal text-sm mt-2">classes: {classes?.length}</p>
        </div>
    )
}

export default SubjectComponent