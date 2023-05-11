import { useContext, useState } from 'react';
import useInterval from '../hooks/useInterval';
import SessionContext from '../context/SessionContext';
import QRCodeComponent from '../components/QRCodeComponent';
import { Student, getQrCode, getStudentsList } from '../services/ApiService';

const delay = 250

const QRCodeAndList = () => {
    const { session, setSession } = useContext(SessionContext);
    const user = JSON.parse(localStorage.getItem('user')!!); // {email: string}

    const [studentList, setStudentList] = useState<
        Student[]
    >([]);

    const stop = useInterval(() => {
        let terminated = false
        getStudentsList(user).then(resp => {
            if (resp.status === 'success') {
                if (resp.data?.terminated) {
                    console.log('TERMINATED')
                    terminated = true
                    const newSession = JSON.parse(JSON.stringify(session));
                    newSession.qrCode = '';
                    setSession(newSession)
                } else {
                    console.log('resp.DATA = ', resp.data)
                    setStudentList(resp.data!!.students!!);
                    // console.log('students: ', resp.data!!.students);
                }
            } else {
                console.log(resp.message);
            }

            if (terminated)
                return
            
            const newSession = JSON.parse(JSON.stringify(session));
            getQrCode({ email: user.email }).then(resp => {
                if (resp.status === 'success') {
                    newSession.qrCode = resp.data!!;
                    console.log('new qr code: ', newSession.qrCode)
                } else {
                    // console.log(`ERROR! message=${resp.message}`);
                }
                setSession(newSession);
                
            })
        });
        
    }, delay);

    return (
        <div className="flex justify-around">
            <div className="text-center">
                <p className="text-xl font-bold underline mb-3">Accounted:</p>
                <ol className="text-center list-decimal text-lg">
                    {studentList.map(s => {
                        return <li key={studentList.indexOf(s)}>{`${s.secondName} ${s.firstName}`}</li>;
                    })}
                </ol>
            </div>
            <QRCodeComponent value={session.qrCode} />
        </div>
    );
};

export default QRCodeAndList;
