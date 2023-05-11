import * as dotenv from 'dotenv';
import express from 'express';
import { Session } from './Session';
import apiRouter from './api';
import cors from 'cors';
import ws from 'express-ws';
import * as http from 'http'

import * as WebSocket from 'ws';

dotenv.config();

let sessions: { [email: string]: Session } = {};

//TODO: create some class for "data" in order to not check if data is formatted correctly every time, but check in one place
//TODO: add endpoint to end session or maybe delete sessions with a timer

// since professor should have ability to cancel processing current student, maybe a session should have
// a list of verified students?

if (!process.env.PORT) {
    console.error('No PORT environment variable!');
    process.exit(1);
}

const PORT = parseInt(process.env.PORT);
const app = express();
const expressWs = ws(app)
const server = http.createServer(app);
const wss = new WebSocket.Server({server,}) // path: "/websocket"
//logging middleware
app.use((req, res, next) => {
    console.log(req.url);
    next();
});
app.use(express.json());
app.use(cors());
app.use('/api', apiRouter);

/**
 * check if there is a session this phone as currentId
 * @param data = "${firstName}:${seconNmae}:${id}"
 * @returns session if there is session containing this data, otherwise null
 */
function getSession(data: string): Session | null {
    for (let email in sessions) {
        let session = sessions[email];
        try {
            if (session.isCurrent(data)) return session;
        } catch (e) {
            return null;
        }
    }
    return null;
}

app.get('/test', (req, res) => {
    res.send(JSON.stringify(sessions));
});

/**
 * return statuses:
 * 200 - session has been created
 * 406 - no email in request
 */
//TODO: handle if session for that email already exists.
app.post('/start', (req, res) => {
    const email = req.body.email
    const subjectName = req.body.subjectName
    console.log(req.body)
    if (!email || !subjectName) {
        res.sendStatus(406);
        return;
    }
    console.log(`/start request from ip: ${req.ip}`);
    sessions[email] = new Session(subjectName);
    res.sendStatus(200);
});

/**
 * return statuses:
 * 201 - name has been put in the session
 * 202 - id has already been scanned (kind of an error)
 * 401 - there is no session for this email
 * 406 - wrong qrCode format or wrong request body
 */
app.post('/scan', (req, res) => {
    if (!req.body.email || !req.body.data) {
        res.sendStatus(406);
        return;
    }
    let email = req.body.email;
    if (!sessions[email]) {
        res.sendStatus(401); //if there is no session for this email
    } else {
        try {
            if (sessions[email].contains(req.body.data)) {
                res.status(202).send('id has already been scanned');
            } else {
                sessions[email].putName(req.body.data);
                res.sendStatus(201);
            }
        } catch (e) {
            res.status(406).send('wrong data format');
        }
    }
});

/**
 * return statuses:
 * 200 - returns token
 * 401 - there is no session for that phone (the phone hasn't been scanned yet!) (or wrong data format)
 * 406 - wrong qrCode format or wrong request body
 */
app.post('/student', (req, res) => {
    if (!req.body.data) {
        res.sendStatus(406);
        return;
    }
    //TODO: data is not verified!
    let data = req.body.data;
    let session = getSession(data);
    console.log("SESSION = ", session)
    if (session !== null) {
        const token = session.getToken()
        console.log("TOKEN = ", session)
        res.status(200).json({ token });
    } else {
        console.log("SENDING 401")
        res.sendStatus(401);
    }
});

/**
 * return statuses:
 * 200 - student is accounted, returns name
 * 401 - token is incorrect!
 * 406 - request message error
 */
app.post('/verify', (req, res) => {
    if (!req.body.email || !req.body.data) {
        res.sendStatus(406);
        return;
    }
    let email = req.body.email;
    let token = req.body.data;
    let session = sessions[email];
    //TODO: chekc if null - than server error
    if (session.getToken() === token) {
        res.status(200).json(session.getCurrentName());
    } else {
        res.sendStatus(401);
    }
});

app.listen(PORT, () => console.log(`started on port ${PORT}`));

/**
 * return statuses:
 * 200 - student is accounted, returns name
 * 202 - id has already been scanned (kind of an error)
 * 406 - request message error
 */
//it doesn't change current Id in session to allow bluetooth and qrcode work in parallel
app.post('/bluetooth', (req, res) => {
    if (!req.body.email || !req.body.data) {
        res.status(406).send('wrong data format');
        return;
    }
    let email = req.body.email;
    if (!sessions[email]) {
        res.sendStatus(401);
    } else {
        try {
            let session = sessions[email];
            if (session.contains(req.body.data)) {
                res.status(202).send('id has already been scanned');
            } else {
                session.saveName(req.body.data);
                res.sendStatus(200);
            }
        } catch (e) {
            res.status(406).send('wrong data format');
        }
    }
});

/**
 * return statuses:
 * 200 - return qr code
 * 406 - no email in request
 * 401 - there is no session for this email
 */
app.post('/qr-code', (req, res) => {
    if (!req.body.email) {
        res.sendStatus(406);
        return;
    }
    const email = req.body.email;
    const session = sessions[email];
    if (!session) {
        res.sendStatus(401); //if there is no session for this email
    } else {
        const qrCode = session.qrCode;
        res.status(200).send(qrCode);
    }
});

/**
 * return statuses:
 * 200 - ok
 * 202 - id already been saved
 * 406 - no qrCode in request
 * 401 - there is no session with such qrCode or wrong qrCode
 */
//TODO: maybe qr code should also display professor's email, so it will be easier to find the right session in this request?
app.post('/student-qr-code', (req, res) => {
    //get any session with such qrCode
    const qrCode = req.body.qrCode;
    const data = req.body.data;
    if (!qrCode || !data) {
        res.sendStatus(406);
        return;
    }
    const s = Object.values(sessions).filter(f => f.qrCode === qrCode);
    if (s.length === 0) {
        res.sendStatus(401);
        return;
    }
    const session = s[0];
    if (session.qrCode !== qrCode) {
        res.sendStatus(401);
        return;
    }
    if (session.contains(req.body.data)) {
        res.sendStatus(202);
        return;
    }
    session.saveName(data);
    res.sendStatus(200);
});

/**
 * End session, get list of students as a result
 * 406 - no email in request
 * 401 - no sessoin with this email
 * 200 - session ended, returns list of students
 */
app.post('/end', (req, res) => {
    if (!req.body.email) {
        res.sendStatus(406);
        return;
    }
    let email = req.body.email;
    if (!sessions[email]) {
        res.sendStatus(401); //if there is no session for this email
        return;
    }
    const result = sessions[email].getListOfStudents()
    delete sessions[email];
    res.json(result);
});

//request returns list of students in the current session.
/**
 * 406 - no email in request
 * 401 - no current session with this email
 */
app.post('/current-students', (req, res) => {
    const email = req.body.email;
    if (!email) {
        res.sendStatus(406);
        return;
    }
    const session = sessions[email];
    if (!session) {
        res.sendStatus(401);
        return;
    }
    
    const result = {
        subjectName: session.subjectName,
        students: session.getListOfStudents()
    };

    res.json(result)
});

// (app as any).ws('/websocket', (ws: WebSocket, req: express.Request) => {
//     console.log('WEBSOCKET')
//     console.log(req.body.email)
// });

// app.ws('/websocket', (ws: WebSocket, req: express.Request) => {
//     console.log('WEBSOCKET')
//     console.log(req.body.email)
// });

wss.on('connection', (ws: WebSocket) => {
    console.log('CONNECTED')
    ws.on('message', msg => {
        console.log('email = ', msg)
        //get session and pass this ws to it.
    })
})

// catch 404 and forward to error handler
app.use(function (req: express.Request, res: express.Response, next) {
    next({
        status: 404,
        message: 'end of endpoint list',
    });
});

app.use(function (
    err: any,
    req: express.Request,
    res: express.Response,
    next: express.NextFunction
) {
    console.error(err);
    res.status(err.status || 500).json();
});
