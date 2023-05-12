import { useLocation, useNavigate } from 'react-router-dom';
import { CheckMethod, Student, Subject, checkMethods } from './SubjectToEdit';
import { useState } from 'react';
import { Listbox } from '@headlessui/react';
import { CheckIcon, ChevronUpDownIcon } from '@heroicons/react/20/solid'
import { updatePredefinedClass } from '../services/ApiService';

const EditClass = () => {
    const { state } = useLocation();
    const navigate = useNavigate();
    const { subject }: { subject: Subject } = state;

    const [name, setName] = useState(subject.subjectName);
    const [method, setMethod] = useState<CheckMethod>(checkMethods[0]);
    const [students, setStudents] = useState<string[]>(subject.students.map(s => `${s.secondName} ${s.firstName}`))

    async function handleSave() {
        const studentList = students.filter(s => s).map(s => {
            const [secondName, firstName, ] = s.split(' ')
            return {
                firstName: firstName || '',
                secondName
            }
        })
        
        const resp = await updatePredefinedClass({
            classId: subject.id,
            subjectName: name,
            method: method,
            studentList
        })

        if (resp.status === 'success') {
            navigate(-1)
        } else {
            console.log('error updating predefined classes! ', resp.message )
        }
    }

    return (
        <div className="container mx-auto">
            <div className="">
                <div className="mb-5">
                    <button
                        className="border-2 p-2"
                        onClick={() => navigate(-1)}
                    >
                        back
                    </button>
                </div>
                <div className="border-4 border-gray-100 p-5">
                    <div className="flex space-x-5">
                        <div>
                            <p className="mb-3">Subject name:</p>
                            <input
                                type="text"
                                value={name}
                                onChange={e => setName(e.target.value)}
                                className="border border-gray-300 focus:border-2 focus:ring-blue-500 focus:border-blue-500 text-lg mb-5 p-1"
                            />
                        </div>
                        <div>
                            <p className="mb-3">Attendance check method:</p>

                            <Listbox value={method} onChange={setMethod}>
                                <div className="relative mt-1">
                                    <Listbox.Button className="relative w-full cursor-default rounded-lg bg-white py-2 pl-3 pr-10 text-left shadow-md focus:outline-none focus-visible:border-indigo-500 focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-opacity-75 focus-visible:ring-offset-2 focus-visible:ring-offset-orange-300 sm:text-sm">
                                        <span className="block truncate">
                                            {method}
                                        </span>
                                        <span className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-2">
                                            <ChevronUpDownIcon
                                                className="h-5 w-5 text-gray-400"
                                                aria-hidden="true"
                                            />
                                        </span>
                                    </Listbox.Button>
                                        <Listbox.Options className="absolute mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 text-base shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none sm:text-sm">
                                            {checkMethods.map((m, i) => (
                                                <Listbox.Option
                                                    key={i}
                                                    className={({ active }) =>
                                                        `relative cursor-default select-none py-2 pl-10 pr-4 ${
                                                            active
                                                                ? 'bg-blue-100 text-blue-900'
                                                                : 'text-gray-900'
                                                        }`
                                                    }
                                                    value={m}
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
                                                                {m}
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

                    <p className="mb-3">Student List:</p>
                    <ol className="list-decimal">
                        {students.map((s, studentIndex) => (
                            <li key={studentIndex}>
                                <input
                                type="text"
                                className="border border-gray-300 focus:border-2 focus:ring-blue-500 focus:border-blue-500 text-lg mb-2 p-1"
                                placeholder='surname name'
                                value={s}
                                onChange={e => {
                                    // const [secondName, firstName] = e.target.value.split(' ')
                                    const newValue = e.target.value
                                    const newArray = students.map((st, i) => {
                                        if (i !== studentIndex)
                                            return st
                                        return newValue
                                    })
                                    setStudents(newArray)
                                }}
                                />
                                <button className="p-2 ml-2 rounded-md bg-red-200"
                                onClick={e => {
                                    const newArray = students.filter((st, i) => i !== studentIndex)
                                    setStudents(newArray)
                                }}>-</button>
                            </li>
                        ))}
                    </ol>
                    <div>
                        <button className="p-2 rounded-md bg-green-200"
                        onClick={() => setStudents([...students, ''])}>
                            add
                        </button>
                    </div>

                    <button className="mt-5 px-5 py-2 w-fit rounded-md bg-blue-200 hover:bg-blue-300"
                    onClick={handleSave}
                    >
                        Save
                    </button>
                </div>
            </div>
        </div>
    );
};

export default EditClass;
