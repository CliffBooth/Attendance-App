import { useLocation, useNavigate } from 'react-router-dom';
import { Class, Student } from '../../services/ApiService';
import { Subject } from '../SubjectToEdit';
import { Square2StackIcon } from '@heroicons/react/20/solid';

interface Props {
    classes: Class[];
}

const AttendanceView = () => {
    function studentToLowerCase(st: { firstName: string; secondName: string }) {
        return {
            ...st,
            firstName: st.firstName.toLowerCase(),
            secondName: st.secondName.toLowerCase(),
        };
    }

    const { state } = useLocation();

    const { classes, predefined }: { classes: Class[]; predefined?: Subject } =
        state;
    classes.forEach(cl => cl.students.map(s => studentToLowerCase(s)));
    if (predefined) {
        predefined.students.map(s => studentToLowerCase(s));
    }

    console.log('ATTENDANCE VIEW: ', predefined);

    const navigate = useNavigate();

    function getTable() {
        console.log(classes);
        const set = new Set<string>();

        const newList: { firstName: string; secondName: string }[] = [];

        if (predefined) {
            for (let i = 0; i < predefined.students.length; i++) {
                const student = predefined.students[i];
                classes.forEach(cl =>
                    cl.students.forEach(st => {
                        if (
                            st.firstName === student.firstName &&
                            st.secondName === student.secondName
                        ) {
                        } else {
                            newList.push({
                                firstName: student.firstName,
                                secondName: student.secondName,
                            });
                        }
                    })
                );
            }
        }

        classes.forEach(c => {
            c.students.forEach(st => {
                const stringified = JSON.stringify(st);
                console.log('class = ', stringified);
                set.add(stringified);
            });
        });

        if (predefined) {
            newList.forEach(s => {
                if (s !== null) {
                    const stringified = JSON.stringify(s);
                    console.log('predefined = ', stringified);
                    set.add(stringified);
                }
            });
        }

        const allStudents: Student[] = Array.from(set.values()).map(el =>
            JSON.parse(el)
        );
        allStudents.sort((a, b) => {
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

        console.log('allStudents: ', allStudents);
        const allDates = classes.map(cl => {
            const d = new Date(cl.date);
            const str = d.toLocaleDateString('ru-RU', {
                day: '2-digit',
                month: '2-digit',
                year: '2-digit',
            });

            return [cl.date, str];
        });

        function equalStudents(s1: Student, s2: Student): boolean {
            if (s1.phoneId && s2.phoneId)
                return (
                    s1.phoneId === s2.phoneId &&
                    s1.firstName === s2.firstName &&
                    s1.secondName === s2.secondName
                );
            else
                return (
                    s1.firstName === s2.firstName &&
                    s1.secondName === s2.secondName
                );
        }

        let currentStudentCounter = 0;

        //divide by subfunctions!!!
        return (
            <div className="overflow-x-auto">
                <table>
                    <tr className="table-row">
                        <th>#</th>
                        <th className="border-2 px-2">Студент</th>
                        {allDates.map(d => (
                            <th className="border-2 px-2 ">{d[1]}</th>
                        ))}
                    </tr>
                    {/* {Array.from(allStudents.values()).map(s => { */}
                    {allStudents.map((s, ind) => {
                        currentStudentCounter = 0;
                        return (
                            <tr className="table-row">
                                <td>{ind + 1}</td>
                                <td className="border-2 p-1">{`${s.secondName} ${s.firstName}`}</td>
                                {allDates.map(d => {
                                    const theClass = classes.filter(
                                        cl => cl.date == d[0]
                                    )[0];
                                    const classIncludesStudent =
                                        theClass.students.some(s1 =>
                                            equalStudents(s, s1)
                                        );

                                    let style = 'bg-red-300';
                                    // let style = ""
                                    if (classIncludesStudent) {
                                        currentStudentCounter++;
                                        style = 'bg-green-500';
                                    }

                                    return (
                                        <td
                                            className={`${style} border-2 border-gray-600 bg-clip-padding`}
                                        ></td>
                                    );
                                })}
                                <td>
                                    {currentStudentCounter}/{allDates.length}
                                </td>
                            </tr>
                        );
                    })}
                </table>
            </div>
        );
    }

    return (
        <div className="container mx-auto">
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
