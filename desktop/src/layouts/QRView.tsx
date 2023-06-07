import { useContext, useEffect, useState } from 'react';
import SessionContext from '../context/SessionContext';
import {
    addSession,
    getPredefinedClasses,
    getQrCode,
    startSession,
    stopSession,
} from '../services/ApiService';
import QRCodeAndList from './QRCodeAndList';
import { Combobox, Listbox } from '@headlessui/react';
import CheckIcon from '@heroicons/react/20/solid/CheckIcon';
import ChevronUpDownIcon from '@heroicons/react/20/solid/ChevronUpDownIcon';
import { useTranslation } from 'react-i18next';

const maxSize = 750;

let i = 1;

// const tempArray = ['one', 'two', 'three'];

const QRView: React.FC = () => {
    /**
     * display loading while loading
    *
    * press button -> start session and then get qr_code or, if session already started, get qr_code
    */
   
   // const [qrCode, setQrCode] = useState<string>('');
   const [inputValue, setInputValue] = useState<string>('');
   const [query, setQuery] = useState('');
   const [subjects, setSubjects] = useState<string[]>([]);
   
   const { session, setSession } = useContext(SessionContext);
   const user = JSON.parse(localStorage.getItem('user')!!);
   
   const {t} = useTranslation();
   const initialOption = t('select the subject');
   const [selected, setSelected] = useState(initialOption);

    useEffect(() => {
        async function fetchSubjects() {
            const resp = await getPredefinedClasses();
            if (resp.status === 'success' && resp.data) {
                const subjectNames = resp.data.map(s => s.subjectName);
                setSubjects(subjectNames);
            } else {
                console.log('error getting predefined classes, ', resp.message);
            }
        }
        fetchSubjects();
    }, []);

    async function handleStart() {
        console.log('start session');
        if (inputValue.length === 0) return;

        const newSession = JSON.parse(JSON.stringify(session));
        newSession.subjectName = inputValue;

        const resp = await startSession({
            email: user.email,
            subjectName: newSession.subjectName,
        });
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
                    date: new Date().getTime(),
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
        setSelected(initialOption);
    }

    return (
        <div className="flex flex-col justify-center items-center">
            {session.qrCode.length !== 0 && (
                // <QRCodeComponent value={session.qrCode} />
                <QRCodeAndList />
            )}
            {session.qrCode.length === 0 && (
                <div className="my-2">
                    <h1><>{t('Subject name')}</>:</h1>
                    <div className="fle flex-col space-x-3 space-y-3">
                        <input
                            type="text"
                            className="border-black border-2 text-center"
                            value={inputValue}
                            onChange={e => setInputValue(e.target.value)}
                        />
                        <Listbox
                            value={selected}
                            onChange={s => {
                                setSelected(s);
                                setInputValue(s);
                            }}
                        >
                            <div className="relative mt-1">
                                <Listbox.Button className="select-none relative w-full cursor-default  bg-white py-2 pl-3 pr-10 text-left shadow-md focus:outline-none focus-visible:border-indigo-500 focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-opacity-75 focus-visible:ring-offset-2 focus-visible:ring-offset-orange-300 sm:text-sm border-2 border-black">
                                    <span className="block truncate">
                                        {selected}
                                    </span>
                                    <span className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-2">
                                        <ChevronUpDownIcon
                                            className="h-5 w-5 text-gray-400"
                                            aria-hidden="true"
                                        />
                                    </span>
                                </Listbox.Button>
                                <Listbox.Options className="absolute mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 text-base shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none sm:text-sm">
                                    {subjects.map((el, i) => (
                                        <Listbox.Option
                                            key={i}
                                            className={({ active }) =>
                                                `relative cursor-default select-none py-2 pl-10 pr-4 ${
                                                    active
                                                        ? 'bg-blue-100 text-blue-900'
                                                        : 'text-gray-900'
                                                }`
                                            }
                                            value={el}
                                        >
                                            {({ selected }) => (
                                                <>
                                                    <span
                                                        className={`block truncate ${
                                                            selected
                                                                ? 'font-medium'
                                                                : 'font-normal'
                                                        }`}
                                                    >
                                                        {el}
                                                    </span>
                                                    {selected ? (
                                                        <span className="absolute inset-y-0 left-0 flex items-center pl-3 text-blue-600">
                                                            <CheckIcon
                                                                className="h-5 w-5"
                                                                aria-hidden="true"
                                                            />
                                                        </span>
                                                    ) : null}
                                                </>
                                            )}
                                        </Listbox.Option>
                                    ))}
                                </Listbox.Options>
                            </div>
                        </Listbox>
                    </div>
                </div>
            )}
            {session.qrCode.length === 0 ? (
                <button
                    className="bg-gray-700 text-xl text-white p-2 rounded-md hover:bg-gray-600"
                    onClick={handleStart}
                >
                    <>{t('Start session')}</>
                </button>
            ) : (
                <button
                    className="bg-gray-700 text-xl text-white p-2 rounded-md hover:bg-gray-600"
                    onClick={endSession}
                >
                    <>{t('End session')}</>
                </button>
            )}
        </div>
    );
};

export default QRView;
