import { useLocation, useNavigate } from 'react-router-dom';
import { Class, Student } from '../../services/ApiService';
import { Subject } from '../SubjectToEdit';

const AttendanceView = () => {
    const { state } = useLocation();

    //classes are already of the same subject
    const { classes, predefined }: { classes: Class[]; predefined?: Subject } =
        state;

    console.log('flat: ', classes.map(cl => cl.students).flat(1));
    // console.log('ATTENDANCE VIEW: ', predefined);

    const navigate = useNavigate();

    function compareByName(s1: { firstName: string; secondName: string }, s2: { firstName: string; secondName: string }) {
        return s1.firstName.toLowerCase() === s2.firstName.toLowerCase() && s1.secondName.toLowerCase() === s2.secondName.toLowerCase()
    }

    function listOfDistinct(list: { firstName: string; secondName: string }[]) {
        const res: { firstName: string; secondName: string }[] = [];
        for (const st of list) {
            if (
                !res.find(s => compareByName(s, st))
            ) {
                console.log('pushing', st)
                res.push(st);
            }
        }
        return res;
    }

    function getTable() {
        console.log(classes);
        // const set = new Set<string>();

        // const newList: { firstName: string; secondName: string }[] = [];

        const fullList: {firstName: string, secondName: string}[] = classes.map(cl => cl.students).flat(1)
        if (predefined) {
            for (const st of predefined.students) {
                fullList.push(st)
            }
        }
        const students = listOfDistinct(fullList)
        console.log('allStudents: ', students);


        students.sort((a, b) => {
            if (a.secondName < b.secondName) {
                return -1;
            } else if (a.secondName > b.secondName) {
                return 1;
            } else if (a.firstName < b.firstName) {
                return -1;
            } else if (a.firstName > b.firstName) {
                return 1;
            }
            return 0;
        });
        
        const allDates = classes.map(cl => {
            const d = new Date(cl.date);
            const str = d.toLocaleDateString('ru-RU', {
                day: '2-digit',
                month: '2-digit',
                year: '2-digit',
            });

            return [cl.date, str];
        });

        let currentStudentCounter = 0;

        //divide by subfunctions!!!
        return (
            <div className="overflow-x-auto">
                <table>
                    <thead>
                        <tr key="tr1" className="table-row">
                            <th>#</th>
                            <th className="border-2 px-2">Студент</th>
                            {allDates.map((d, ind) => (
                                <th
                                    key={`date-${ind}`}
                                    className="border-2 px-2 "
                                >
                                    {d[1]}
                                </th>
                            ))}
                        </tr>
                    </thead>
                    {/* {Array.from(allStudents.values()).map(s => { */}
                    <tbody>
                        {students.map((s, ind) => {
                            currentStudentCounter = 0;
                            return (
                                <tr key={ind} className="table-row">
                                    <td key={`1-${ind}`}>{ind + 1}</td>
                                    <td
                                        key={`2-${ind}`}
                                        className="border-2 p-1"
                                    >{`${s.secondName} ${s.firstName}`}</td>
                                    {allDates.map((d, dInd) => {
                                        const theClass = classes.filter(
                                            cl => cl.date == d[0]
                                        )[0];
                                        const classIncludesStudent =
                                            theClass.students.some(s1 =>
                                                compareByName(s, s1)
                                            );

                                        let style = 'bg-red-300';
                                        // let style = ""
                                        if (classIncludesStudent) {
                                            currentStudentCounter++;
                                            style = 'bg-green-500';
                                        }

                                        return (
                                            <td
                                                key={`3-${ind}-${dInd}`}
                                                className={`${style} border-2 border-gray-600 bg-clip-padding`}
                                            ></td>
                                        );
                                    })}
                                    <td key={`4-${ind}`}>
                                        {currentStudentCounter}/
                                        {allDates.length}
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>
        );
    }

    return (
        <div className="container mx-auto px-5">
            <div className="relative flex mb-3 justify-center">
                <button
                    onClick={() => navigate(-1)}
                    className="absolute left-0 border-2 p-2"
                >
                    back
                </button>
                <h1 className="font-bold text-3xl text-center">
                    {classes[0].subjectName}
                </h1>
            </div>
            {getTable()}
        </div>
    );
};

export default AttendanceView;
