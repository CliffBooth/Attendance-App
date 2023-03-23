import { useLocation, useNavigate } from 'react-router-dom';
import { Class } from '../../services/ApiService';

interface Props {
    classes: Class[];
}

interface Student {
    first_name: string,
    second_name: string,
    email: string
}

const AttendanceView = () => {

    const { state } = useLocation();

    const { classes }: {classes: Class[]} = state;

    const navigate = useNavigate();
    
    function getTable() {
        console.log(classes)
        const set = new Set<string>()
        classes.forEach((c) => {
            c.students.forEach(st => set.add(JSON.stringify(st)))
        })
        const allStudents = Array.from(set.values()).map(el => JSON.parse(el))
        allStudents.sort((a, b) => {
            if (a.second_name < b.second_name) {
                return -1
            } else if (a.second_name > b.second_name) {
                return 1
            } else if  (a.first_name < b.first_name) {
                return -1
            } else if (a.first_name > b.first_name) {
                return 1
            }
            return 0
        })

        console.log('allStudents: ', allStudents)
        const allDates = classes.map(cl => {
            const d = new Date(cl.date)
            const str = d.toLocaleDateString('ru-RU', {
                day: '2-digit',
                month: '2-digit',
                year: '2-digit',
            })

            return [cl.date, str]
        })

        function equalStudents(s1: Student, s2: Student): boolean {
            return s1.email === s2.email && 
                s1.first_name === s2.first_name &&
                s1.second_name === s2.second_name
        }

        let currentStudentCounter = 0

        //divide by subfunctions!!!
        return (
            <div className='overflow-x-auto'>
                <table>
                    <tr className='table-row'>
                        <th>#</th>
                        <th className='border-2 px-2'>Студент</th>
                        {allDates.map(d => <th className='border-2 px-2 '>{d[1]}</th>)}
                    </tr>
                    {/* {Array.from(allStudents.values()).map(s => { */}
                    {allStudents.map((s, ind) => {
                        currentStudentCounter = 0
                        return ( 
                            <tr className='table-row'>
                                <td>{ind + 1}</td>
                                <td className='border-2 p-1'>{`${s.second_name} ${s.first_name}`}</td>
                                {allDates.map(d => {
                                    const theClass = classes.filter(cl => cl.date == d[0])[0]
                                    const classIncludesStudent = theClass.students.some(s1 => equalStudents(s, s1))
                                    
                                    let style = 'bg-red-300'
                                    // let style = ""
                                    if (classIncludesStudent) {
                                        currentStudentCounter++
                                        style = 'bg-green-500'
                                    }

                                    return (
                                        <td className={`${style} border-2 border-gray-600 bg-clip-padding`}></td>
                                    )
                                })}
                                <td>{currentStudentCounter}/{allDates.length}</td>
                            </tr>
                        );
                    })}
                </table>
            </div>
        );
    }

    return (
    <div className='container mx-auto'>
        <div className='relative flex mb-3 justify-center'>
            <button 
                onClick={() => navigate(-1)}
                className='absolute left-0 border-2 p-2'
            >back</button>
            <h1 className="font-bold text-3xl text-center">{classes[0].subject_name}</h1>
        </div>
        {getTable()}
    </div>
    );
};

export default AttendanceView;
