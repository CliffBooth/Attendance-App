import { useContext, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import UserContext from '../context/UserContext';
import ULink from './MyLink';
import { useTranslation } from 'react-i18next';
import { Dialog, Listbox } from '@headlessui/react';
import {
    CheckIcon,
    ChevronUpDownIcon,
    Cog6ToothIcon,
} from '@heroicons/react/20/solid';

const languages: {[k: string]: string} = {
    'en': 'English',
    'ru': 'Русский'
}

function NavComponent() {
    const navigate = useNavigate();

    const { user, setUser } = useContext(UserContext);
    const language = localStorage.getItem('lang') ?? 'en';
    const [selected, setSelected] = useState(language);

    const { t, i18n } = useTranslation();

    useEffect(() => {
        changeLanguage(language);
    }, []);

    function changeLanguage(lang: string) {
        const key = Object.keys(languages).find(k => languages[k] === lang) ?? 'en'
        console.log('selected lang: ', key)
        i18n.changeLanguage(key);
        localStorage.setItem('lang', key);
    }

    const [dialogOpened, setDialogOpened] = useState(false);

    function handleCloseDialog() {
        setDialogOpened(false);
    }

    function openDialog() {
        setDialogOpened(true)
    }

    const data = window.localStorage.getItem('user');
    let user1: { email: string } = { email: '' };
    if (data) user1 = JSON.parse(data);

    function handleLogout() {
        setUser({
            loggedIn: false,
            email: '',
        });
        window.localStorage.removeItem('user');
        navigate('sign-in');
    }

    return (
        <div className="relative container mx-auto p-6 shadow-lg mb-5 bg-[#F7F7F7]">
            <div className="flex items-center justify-between">
                <button>
                    <Cog6ToothIcon className="w-9" onClick={openDialog} />
                </button>
                {user1.email && (
                    <>
                        <div className="font-bold text-2xl text-center p-2 grow space-x-6">
                            <ULink to="/">{t('Home')}</ULink>
                            <ULink to="qr">{t('Start class')}</ULink>
                        </div>
                        <button
                            onClick={handleLogout}
                            className="bg-gray-300 rounded-md p-2 justify-self-end"
                        >
                            <>{t('log out')}</>
                        </button>
                    </>
                )}
            </div>

            <Dialog
                open={dialogOpened}
                onClose={handleCloseDialog}
                className="relative z-50"
            >
                <div className="fixed inset-0 bg-black/30">
                    <div className="fixed inset-0 flex items-center justify-center p-4">
                        <Dialog.Panel className="p-5 flex flex-col w-full max-w-sm rounded bg-white">
                            <Dialog.Title className="text-center mb-4 font-bold">
                                <>{t('Settings')}</>:
                            </Dialog.Title>
                            <div><>{t('Language')}</>:</div>
                            <Listbox
                                value={language}
                                onChange={s => {
                                    setSelected(s);
                                    // setInputValue(s);
                                    changeLanguage(s);
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
                                        {Object.keys(languages).map((el, i) => (
                                            <Listbox.Option
                                                key={i}
                                                className={({ active }) =>
                                                    `relative cursor-default select-none py-2 pl-10 pr-4 ${
                                                        active
                                                            ? 'bg-blue-100 text-blue-900'
                                                            : 'text-gray-900'
                                                    }`
                                                }
                                                value={languages[el]}
                                            >
                                                {({ selected }) => (
                                                    <>
                                                        <span
                                                            className={`block ${
                                                                selected
                                                                    ? 'font-medium'
                                                                    : 'font-normal'
                                                            }`}
                                                        >
                                                            {languages[el]}
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
                        </Dialog.Panel>
                    </div>
                </div>
            </Dialog>
        </div>
    );
}

export default NavComponent;
