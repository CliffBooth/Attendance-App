import { createContext, Dispatch, SetStateAction } from 'react';

export interface Session {
    qrCode: string,
    subjectName: string
}

export interface Ctx {
    session: Session,
    setSession: Dispatch<SetStateAction<Session>>;
}

const SessionContext = createContext<Ctx>({
    session: {
        qrCode: "",
        subjectName: ""
    },
    setSession: () => {}
});

export default SessionContext