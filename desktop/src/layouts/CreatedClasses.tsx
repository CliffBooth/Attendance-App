import { useEffect, useState } from 'react';
import { Dialog } from '@headlessui/react';
import SubjectToEdit, { Subject, checkMethods } from '../components/SubjectToEdit';
import { addPredefinedClass, getPredefinedClasses } from '../services/ApiService';

type AuthMethod = 'any' | 'phone qr code' | 'screen qr code' | 'bluetooth'

const CreatedClasses = () => {
    const [subjects, setSubjects] = useState<Subject[]>([]);
    const [dialogOpened, setOpened] = useState(false);
    const [dialogSubjectName, setSubjectName] = useState('')

    function handleCloseDialog() {
        setOpened(false);
    }

    async function fetchSubjects() {
        const resp = await getPredefinedClasses()
        if (resp.status === 'success') {
            setSubjects(resp.data || [])
        } else {
            console.log('cannot fetch classes ', resp.message)
        }
    }

    useEffect(() => {
        fetchSubjects()
    }, [])

    async function onSaveSubject() {
        //TODO: make an api request i guess
        const resp = await addPredefinedClass({
            subjectName: dialogSubjectName,
            method: checkMethods[0]
        })

        await fetchSubjects();

        setSubjectName('')
        setOpened(false);
    }

    return (
        <div className="min-h-[200px] space-y-3 ">
            {subjects.map(s => (
                <div className="flex">
                    <SubjectToEdit className="grow" subject={s}/>
                    <button className="p-2 ml-2 rounded-md bg-red-200"
                    onClick={e => {
                        // const newArray = students.filter((st, i) => i !== studentIndex)
                        // setStudents(newArray)
                    }}>-</button>
                </div>
            ))}
            <div className="flex justify-end ">
                <button
                    className="flex justify-center items-center p-5 w-14 h-14 text-bold text border-2 border-black rounded-[50%] bg-green-400 shadow transition hover:-translate-x-0.5 hover:-translate-y-0.5 hover:scale-[.95]"
                    onClick={() => setOpened(true)}
                >
                    <p>+</p>
                </button>
            </div>

            <Dialog
                open={dialogOpened}
                onClose={handleCloseDialog}
                className="relative z-50"
            >
                <div className="fixed inset-0 bg-black/30">
                    <div className="fixed inset-0 flex items-center justify-center p-4">
                        <Dialog.Panel className="p-5 flex flex-col w-full max-w-sm rounded bg-white">
                            <Dialog.Title className="text-center mb-4">Enter the subject name:</Dialog.Title>
                            <input
                                type="text"
                                value={dialogSubjectName}
                                className="border border-gray-300 focus:border-2 bg-gray-100 focus:ring-blue-500 focus:border-blue-500 text-lg mb-5 p-1"
                                onChange={e => setSubjectName(e.target.value)}
                             />
                             <div className='flex space-x-6'>
                            <button 
                            onClick={onSaveSubject}
                            className="px-5 py-2 w-fit rounded-md bg-blue-200 hover:bg-blue-300"
                            >
                                Save
                            </button>
                            <button
                            onClick={handleCloseDialog}
                            className="px-5 py-2 w-fit rounded-md bg-red-200 hover:bg-red-300"
                            >
                                Cancel
                            </button>
                            </div>
                        </Dialog.Panel>
                    </div>
                </div>
            </Dialog>
        </div>
    );
};
//hover:-translate-x-1 hover:-translate-y-1
export default CreatedClasses;
