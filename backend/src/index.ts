import * as dotenv from 'dotenv';
import express from 'express';
import { Session } from './Session';
import apiRouter from './api';
import cors from 'cors'

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
//logging middleware
app.use((req, res, next) => {
    console.log(req.url)
    next()
})
app.use(express.json());
app.use(cors())
app.use('/api', apiRouter);


/**
 * chekc if there is a session this phone as currentId
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
    console.log('test');
    res.send(JSON.stringify(sessions));
});

/**
 * return statuses:
 * 201 - session has been created
 * 406 - no email in request
 */
//TODO: handle if session for that email already exists.
app.post('/start', (req, res) => {
    if (!req.body.email) {
        res.status(406);
        return;
    }
    let email = req.body.email;
    console.log(`/start request from ip: ${req.ip}`);
    sessions[email] = new Session();
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
    console.log('/scan');
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
    console.log('/student');
    if (!req.body.data) {
        res.sendStatus(406);
        return;
    }
    //TODO: data is not verified!
    let data = req.body.data;
    let session = getSession(data);
    if (session !== null) {
        res.status(200).json({ token: session.getToken() });
    } else {
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
    console.log('/verify');
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
    console.log('/bluetooth');
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


// catch 404 and forward to error handler
app.use(function (req: express.Request, res: express.Response, next) {
    next({ status: 404 });
});

app.use(function (err: any, req: express.Request, res: express.Response, next: express.NextFunction) {
    console.error(err);
    res.status(err.status || 500).json();
});