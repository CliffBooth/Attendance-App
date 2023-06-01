import App from '../src/app'
import request from 'supertest'
import { describe, expect, test, afterAll, beforeEach } from '@jest/globals';

let app = App()

beforeEach(() => {
    app = App()
})

describe('test /start', () => {
    test('test multiple sessoins', async () => {
        const email1 = "test@mail.com"
        const email2 = "test2@mail.com"
        const subjectName1 = "test-subjcect"
        const subjectName2 = "test-subjcect2"
        await request(app)
            .post('/start')
            .send({
                email: email1,
                subjectName: subjectName1,
            })
            .expect(200)
        await request(app)
        .post('/start')
        .send({
            email: email2,
            subjectName: subjectName2,
        })
        .expect(200)

        await registerStudent(email1, "student:one:123")

        const result = await getStudents(email1)
        expect(await getStudents(email1)).toStrictEqual({
            subjectName: subjectName1,
            students: [
                {
                    firstName: "student",
                    secondName: "one",
                    phoneId: "123"
                }
            ]
        })

        expect(await getStudents(email2)).toStrictEqual({
            subjectName: subjectName2,
            students: []
        })
        
        })

    test('wrong body format', async () => {
        await request(app)
            .post('/start')
            .send({
                email: "test@mail.com",
            })
            .expect(406)
        await request(app)
        .post('/start')
        .send({
            subjectName: "test-subjcect",
        })
        .expect(406)
    })
})

describe('test student qr registration', () => {
    const email = "test@email.com"
    const data = "student:one:123"
    test('correct flow', async () => {
        await request(app)
            .post('/start')
            .send({
                email,
                subjectName: "test-subjcect",
            })
            .expect(200)

        await registerStudent(email, data)
    })

    test('send the same id twice', async () => {
        await request(app)
            .post('/start')
            .send({
                email,
                subjectName: "test-subjcect",
            })
            .expect(200)
            
        await registerStudent(email, data)

        await request(app)
            .post('/scan')
            .send({
                email,
                data,
            })
            .expect(202)
    })

})

describe('test professor qr code registration', () => {
    const email = "1234@mail.com"
    const subjectName = "test-subjcect"
    const student = {
        firstName: "student",
        secondName: "one",
        phoneId: "123"
    }
    test('wrong QRcode', async () => {
        await request(app)
            .post('/start')
            .send({
                email,
                subjectName,
            })
            .expect(200)
        let res = await request(app)
            .post('/qr-code')
            .send({email})
            .expect(200)
        const qrCode = res.body
        await new Promise<void>(resolve => setTimeout(() => resolve(), 600))
        await request(app)
            .post('/student-qr-code')
            .send({qrCode, data: `${student.firstName}:${student.secondName}:${student.phoneId}`})
            .expect(401)
    })
})

describe('test bluetooth registration', () => {
    const email = "123@mail.com"
    const subjectName = "test-subjcect"
    const student = {
        firstName: "student",
        secondName: "one",
        phoneId: "123"
    }
    test('correct flow', async () => {
        await request(app)
            .post('/start')
            .send({
                email,
                subjectName,
            })
            .expect(200)
        await request(app)
            .post('/bluetooth')
            .send({email, data: `${student.firstName}:${student.secondName}:${student.phoneId}`})
            .expect(200)
        const result = await getStudents(email) 
        expect(result).toStrictEqual({
            subjectName: "test-subjcect",
            students: [student]
        })
    })
})

describe('test adding and deleting student manually', () => {
    const email = "1234@mail.com"
    const subjectName = "test-subjcect"
    const student = {
        firstName: "student",
        secondName: "one",
        phoneId: null,
    }

    test('correct flow', async () => {
        await request(app)
            .post('/start')
            .send({
                email,
                subjectName,
            })
            .expect(200)
        await request(app)
            .post('/add-student')
            .send({student, email})
            .expect(200)
        // console.log("====state==== state = ", (await getState()))
        expect(await getStudents(email)).toStrictEqual({
                subjectName: subjectName,
                students: [student]
            })
        await request(app)
            .post('/delete-student')
            .send({student, email})
            .expect(200)
        expect(await getStudents(email)).toStrictEqual({
            subjectName: subjectName,
            students: []
        })
    })
})

async function registerStudent(email: string, data: string) {
    await request(app)
        .post('/scan')
        .send({
            email,
            data,
        })
        .expect(201)
    let res = await request(app)
        .post('/student')
        .send({data})
        .expect(200)
    const token = res.body.token
    await request(app)
        .post('/verify')
        .send({
            email,
            data: token
        })
        .expect(200)
}

async function getStudents(email: string) {
    const res = await request(app)
        .post('/current-students')
        .send({email})
        .expect(200)
    return res.body
}

async function getState() {
    const res = await request(app)
        .get('/test')
    return res.body
}