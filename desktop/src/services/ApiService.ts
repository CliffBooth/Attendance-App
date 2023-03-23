import axios from 'axios';
import { useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import appConfig from '../configs/appConfig';
import UserContext from '../context/UserContext';

const client = axios.create({
    baseURL: appConfig.apiUrl,
});

interface signInData {
    email: string;
}

interface Result<T> {
    status: 'success' | 'failure';
    message: string;
    data?: T;
}

export default function useAuth() {
    const navigate = useNavigate();

    async function signIn(data: signInData): Promise<Result<null>> {
        try {
            const resp = await client({
                method: 'post',
                url: '/api/login_professor',
                data: data,
            });

            if (resp.status === 200) {
                window.localStorage.setItem('user', JSON.stringify(data));
                navigate('/');
                return {
                    status: 'success',
                    message: '',
                };
            } else {
                return {
                    status: 'failure',
                    message: resp.statusText,
                };
            }
        } catch (errors: any) {
            return {
                status: 'failure',
                message: errors?.response?.data?.message || errors.toString(),
            };
        }
    }

    // async function signUp(): Promise<Result> {

    // }

    return {
        signIn,
    };
}

export interface Class {
    subject_name: string;
    date: string;
    students: {
        email: string;
        first_name: string;
        second_name: string;
    }[];
}

export async function getClasses(user: {
    email: string;
}): Promise<Result<Class[]>> {
    console.log('making a request!');
    console.log(user);

    try {
        const resp = await client({
            url: `/api/professor_classes/${user.email}`,
            method: 'get',
        });

        if (resp.status == 200) {
            return {
                status: 'success',
                message: '',
                data: resp.data,
            };
        } else {
            return {
                status: 'failure',
                message: resp.statusText,
            };
        }
    } catch (errors: any) {
        console.log('ERROR');
        return {
            status: 'failure',
            message: errors?.response?.data?.message || errors.toString(),
        };
    }
}

export async function startSession(data: { email: string }) {
    try {
        const resp = await client({
            url: '/start',
            method: 'post',
            data,
        });
        if (resp.status >= 200 && resp.status < 300) {
            return {
                status: 'success',
                message: '',
            };
        } else {
            return {
                status: 'failure',
                message: `response status = ${resp.status}`,
            };
        }
    } catch (errors: any) {
        return {
            status: 'failure',
            message: errors?.response?.data?.message || errors.toString(),
        };
    }
}

export async function getQrCode(data: {
    email: string;
}): Promise<Result<string>> {
    try {
        const resp = await client({
            url: '/qr-code',
            method: 'post',
            data,
        });
        if (resp.status >= 200 && resp.status < 300) {
            return {
                status: 'success',
                message: '',
                data: resp.data,
            };
        } else {
            return {
                status: 'failure',
                message: `response status = ${resp.status}`,
            };
        }
    } catch (errors: any) {
        return {
            status: 'failure',
            message: errors?.response?.data?.message || errors.toString(),
        };
    }
}

export async function stopSession(data: { email: string }): Promise<
    Result<
        {
            email: string;
            first_name: string;
            second_name: string;
        }[]
    >
> {
    try {
        const resp = await client({
            url: '/end',
            method: 'post',
            data,
        });
        if (resp.status >= 200 && resp.status < 300) {
            return {
                status: 'success',
                message: '',
                data: resp.data,
            };
        } else {
            return {
                status: 'failure',
                message: `response status = ${resp.status}`,
            };
        }
    } catch (errors: any) {
        return {
            status: 'failure',
            message: errors?.response?.data?.message || errors.toString(),
        };
    }
}

//api call to add session to the database
export async function addSession(
    data: {
        subject_name: string;
        students: {
            email: string;
            first_name: string;
            second_name: string;
        }[];
    },
    user: { email: string }
) {
    try {
        const resp = await client({
            url: `/api/professor_classes/${user.email}`,
            method: 'post',
            data,
        });
        if (resp.status >= 200 && resp.status < 300) {
            return {
                status: 'success',
                message: '',
                data: resp.data,
            };
        } else {
            return {
                status: 'failure',
                message: `response status = ${resp.status}`,
            };
        }
    } catch (errors: any) {
        return {
            status: 'failure',
            message: errors?.response?.data?.message || errors.toString(),
        };
    }
}