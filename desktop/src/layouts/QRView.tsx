import { useContext, useState } from 'react';
import SessionContext from '../context/SessionContext';
import {
    addSession,
    getQrCode,
    startSession,
    stopSession,
} from '../services/ApiService';
import QRCodeAndList from './QRCodeAndList';

const maxSize = 750;

let i = 1

const QRView: React.FC = () => {
    /**
     * display loading while loading
     *
     * press button -> start session and then get qr_code or, if session already started, get qr_code
     */

    // const [qrCode, setQrCode] = useState<string>('');
    const [inputValue, setInputValue] = useState<string>('');

    const { session, setSession } = useContext(SessionContext);
    const user = JSON.parse(localStorage.getItem('user')!!);

    async function handleStart() {
        console.log('start session');
        if (inputValue.length === 0) return;

        const newSession = JSON.parse(JSON.stringify(session));
        newSession.subjectName = inputValue;

        const resp = await startSession({ email: user.email, subjectName: newSession.subjectName});
        if (resp.status === 'success') {
            const resp1 = await getQrCode({ email: user.email });
            if (resp1.status === 'success') {
                newSession.qrCode = resp1.data!!;
            } else {
                console.log(`ERROR! message=${resp.message}`);
            }
        } else {
            console.log(`ERROR! message=${resp.message}`);
        }
        setSession(newSession);
    }

    async function endSession() {
        // setQrCode('')
        //request... to get a list of students
        //make api request to ste the list in the database
        const resp = await stopSession({ email: user.email });
        if (resp.status === 'success') {
            console.log('resp.data = ', resp.data);
            if (resp.data!!.length !== 0) {
                const data = {
                    subjectName: session.subjectName,
                    students: resp.data!!,
                };
                const resp1 = await addSession(data, user);
                if (resp1.status === 'failure') {
                    console.log(`ERRRO message = ${resp.message}`);
                }
            }
        } else {
            console.log(`ERROR! message=${resp.message}`);
        }

        setSession({
            subjectName: '',
            qrCode: '',
        });
        // session.qrCode = ''
        setInputValue('');
    }

    return (
            <div className="flex flex-col justify-center items-center">
                {session.qrCode.length !== 0 && (
                    // <QRCodeComponent value={session.qrCode} />
                    <QRCodeAndList />
                )}
                {session.qrCode.length === 0 && (
                    <div className="my-2">
                        <h1>Class name:</h1>
                        <input
                            type="text"
                            className="border-black border-2 text-center"
                            value={inputValue}
                            onChange={e => setInputValue(e.target.value)}
                        />
                    </div>
                )}
                {session.qrCode.length === 0 ? (
                    <button
                        className="bg-gray-700 text-xl text-white p-2 rounded-md hover:bg-gray-600"
                        onClick={handleStart}
                    >
                        Start session
                    </button>
                ) : (
                    <button
                        className="bg-gray-700 text-xl text-white p-2 rounded-md hover:bg-gray-600"
                        onClick={endSession}
                    >
                        End Session
                    </button>
                )}
            </div>
    );
};

export default QRView;
