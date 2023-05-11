import axios from 'axios';
import { useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import appConfig from '../configs/appConfig';
import UserContext from '../context/UserContext';

const client = axios.create({
    baseURL: appConfig.apiUrl,
});

client.interceptors.request.use(config => {
    const token = localStorage.getItem(appConfig.tokenStorageKey)
    if (token) {
        config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
})

interface signInData {
    email: string;
    password: string;
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

            const token = resp.data.token
            if (resp.status === 200 && token) {
                localStorage.setItem(appConfig.tokenStorageKey, token)
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

export interface Student {
    phoneId: string,
    firstName: string,
    secondName: string,
}

export interface Class {
    subjectName: string;
    date: string;
    students: Student[];
}

export async function getClasses(user: {
    email: string;
}): Promise<Result<Class[]>> {
    console.log('making get classes request');
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

export async function startSession(data: { email: string, subjectName: string }) {
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
        Student[]
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
        subjectName: string;
        students: Student[];
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

export async function getStudentsList(data: {email: string}): Promise<Result<{
    subjectName?: string,
    students?: Student[],
    terminated?: boolean,
}>> {
    try {
        const resp = await client({
            url: '/current-students',
            method: 'post',
            data,
        });
        console.log("status = ", resp.status)
        if (resp.status >= 200 && resp.status < 300) {
            return {
                status: 'success',
                message: '',
                data: resp.data,
            };
        } else if (resp.status === 401) {
            console.log("401!!!!!!")
            return {
                status: 'success',
                message: '',
                data: {
                    terminated: true,
                }
            }
        } else {
            return {
                status: 'failure',
                message: `response status = ${resp.status}`,
            };
        }
    } catch (errors: any) {
        if (errors?.response.status === 401) {
            return {
                status: 'success',
                message: '',
                data: {
                    terminated: true,
                }
            }
        }
        return {
            status: 'failure',
            message: errors?.response?.data?.message || errors.toString()
        }
    }
}