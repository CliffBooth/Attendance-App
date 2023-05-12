import { useNavigate } from "react-router-dom"

export const checkMethods = ['any', 'phone qr code', 'screen qr code', 'bluetooth'] as const
export type CheckMethod = typeof checkMethods[number]
export type Student = {firstName: string, secondName: string}

export interface Subject {
    id: number,
    subjectName: string,
    students: Student[],
    method: {
        name: CheckMethod
    }
}

interface Props {
    subject: Subject
    className?: string
}

const SubjectToEdit = ({
    subject,
    className
}: Props) => {

    const navigate = useNavigate();

    function handleClick() {
        console.log('handling click')
        console.log('subject = ', subject)
        navigate('/editClass', {state: {subject}})
    }
    
    return <div 
    className={`${className ? className : ''} flex items-center space-x-2 border-2 shadow-lg px-1 bg-gray-100 transition hover:scale-[1.01]`}
    onClick={handleClick}>
        <div className="h-12 font-bold bg-white px-5 flex justify-center items-center">
            <p className="">{subject.subjectName}</p>
        </div>
        <p className="">students: {subject.students.length}</p>
    </div>
}

export default SubjectToEdit