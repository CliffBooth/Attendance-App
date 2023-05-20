import { useEffect, useState } from 'react';
import { Class, getClasses } from '../services/ApiService';
import SubjectComponent from '../components/bySubject/SubjectComponent';

const ClassesScreen = () => {
    const [classes, setClasses] = useState<Class[]>([]);

    // const {user} = useContext(UserContext)

    console.log('classesScreen rendered!');

    useEffect(() => {
        const data = localStorage.getItem('user');
        if (!data) {
            console.log('local storage is empty!');
            return;
        }
        const user = JSON.parse(data);
        const update = async () => {
            const result = await getClasses(user);
            if (result.status == 'failure') {
                console.log(`failure: ${result.message}`);
            } else {
                console.log('success!');
                console.log(result.data);
                setClasses(result.data!!);
            }
        };
        update();
    }, []);

    function classesByDate() {
        const dateToClasses: { [key: string]: Class[] } = {};

        for (const c of classes) {
            const date = new Date(c.date);
            const dateStr = `${date.getFullYear} ${date.getMonth} ${date.getDate}`;
            if (!dateToClasses[dateStr]) {
                dateToClasses[dateStr] = [c];
            } else {
                dateToClasses[dateStr].push(c);
            }
        }

        return <></>;
    }

    function classesBySubject() {
        console.log('classesBySubject!');
        const subjectToClasses: { [key: string]: Class[] } = {};
        for (const c of classes) {
            if (!subjectToClasses[c.subjectName]) {
                subjectToClasses[c.subjectName] = [c];
            } else {
                subjectToClasses[c.subjectName].push(c);
            }
        }
        console.log(subjectToClasses);
        return (
            <div className='flex space-x-5 flex-wrap space-y-3'>
                {Object.entries(subjectToClasses).map((value, ind) => {
                    return (
                        <SubjectComponent
                            key={ind}
                            subject_name={value[0]}
                            classes={value[1]}
                        />
                    );
                })}
            </div>
        );
    }

    return (
        <div className="container mx-auto">
            {/* {classes.length != 0 && classesByDate()} */}
            {classes.length != 0 && classesBySubject()}
        </div>
    );
};

export default ClassesScreen;
