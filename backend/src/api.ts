import express from 'express';
import { PrismaClient, Student } from '@prisma/client';
import { Request } from 'express';

const router = express.Router();
const prisma = new PrismaClient();

router
    .route('/professor_classes/:email')
    /**
     * get class objects, which contain attendees by professor email
     *
     * returns array of classes if professor have any
     * returns empty array if professro doesn't have any classes
     * reurns status 404 if there is no such email
     */
    //maybe should return student's email as response too, to indicate whether student was created manually?
    .get(async (req, res) => {
        const email = req.params.email;
        const result = await prisma.professor.findUnique({
            where: {
                email,
            },
            include: {
                classes: {
                    select: {
                        students: {
                            select: {
                                email: true,
                                first_name: true,
                                second_name: true,
                            },
                        },
                        subject_name: true,
                        date: true,
                    },
                },
            },
        });

        if (result === null) res.sendStatus(404);
        else res.json(result.classes);
    })
    //save a new class object with professor's email
    /**
     * empty string date => defualt date.
     * student.email == null => create new student
     */
    .post(async (req: Request<{ email: string }, {}, RequestClass>, res) => {
        console.log('POST_SESSION')
        const prof_email = req.params.email;
        //check if professor with such email exists:
        const prof = await prisma.professor.findUnique({
            where: {
                email: prof_email,
            },
        });

        if (prof === null) {
            console.log('prof === null')
            res.sendStatus(404);
            return;
        }

        const data = req.body;

        console.log(`POST /professor_classes/${prof_email}`, data)

        if (data.students === undefined || data.subject_name === undefined) {
            res.sendStatus(406);
            return;
        }

        console.log('running');

        const students: Student[] = [];

        for (let s of data.students) {
            if (s.email != undefined && s.email != null) {
                console.log('s.email != undefined && s.email != null')
                const student = await prisma.student.findUnique({
                    where: {
                        email: s.email,
                    },
                });
                if (student != null) {
                    students.push(student);
                } else {
                    console.log(`non existant student id (email): ${s.email}`)
                }
            } else if (s.first_name != undefined && s.second_name != undefined) {
                console.log('s.first_name != undefined && s.second_name != undefined')
                const student = await prisma.student.create({
                    data: {
                        first_name: s.first_name,
                        second_name: s.second_name,
                    },
                });
                students.push(student);
            }
        }

        // console.log(students);

        const result = await prisma.class.create({
            data: {
                subject_name: data.subject_name,
                professor: {
                    connect: {
                        email: prof_email,
                    },
                },
                students: {
                    connect: students.map(s => ({ id: s.id })),
                },
            },
            include: {
                students: true,
            },
        });

        res.json(result);
    });

interface RequestClass {
    subject_name: string;
    students: RequestStudent[];
}

type RequestStudent = {
    email?: string;
    first_name?: string;
    second_name?: string;
};

//create professor (takes email)
/**
 * returns:
 *  200 if created
 *  406 if incorrect body
 *  409 if email already exists
 */
router.post('/signup_professor', async (req, res) => {
    const email = req.body.email;
    if (!email) {
        res.sendStatus(406);
        return;
    }
    const existing = await prisma.professor.findUnique({
        where: {
            email: email,
        },
    });

    if (existing != null) {
        res.sendStatus(409);
        return;
    }

    const prof = await prisma.professor.create({
        data: {
            email: email,
        },
    });

    res.json(prof);
});
//create student (takes email)
/**
 * returns:
 *  200 if created
 *  406 if incorrect body
 *  409 if email already exists
 */
router.post(
    '/signup_student',
    async (req: Request<{}, {}, BodyStudentSignup>, res) => {
        const data = req.body;
        if (!data.email || !data.firstName || !data.secondName) {
            res.sendStatus(406);
            return;
        }

        const existing = await prisma.student.findUnique({
            where: {
                email: data.email,
            },
        });

        if (existing != null) {
            res.sendStatus(409);
            return;
        }

        const student = await prisma.student.create({
            data: {
                email: data.email,
                first_name: data.firstName,
                second_name: data.secondName,
            },
        });

        res.json(student);
    }
);

interface BodyStudentSignup {
    email: string;
    firstName: string;
    secondName: string;
}
//login: check if professor with such email exists
/**
 * returns:
 *  200 if logged in
 *  406 if body doesn't contain "email"
 *  401 if no such email
 */
router.post('/login_professor', async (req, res) => {
    const email = req.body.email;
    if (!email) {
        res.sendStatus(406);
        return;
    }
    const professor = await prisma.professor.findUnique({
        where: {
            email,
        },
    });
    if (professor != null) res.sendStatus(200);
    else res.sendStatus(401);
});
//login: check if student with such email exists
/**
 * returns:
 *  200 if logged in
 *   406 if body doesn't contain "email"
 *  401 if no such email
 */
router.post('/login_student', async (req, res) => {
    const email = req.body.email;
    if (!email) {
        res.sendStatus(406);
        return;
    }
    const student = await prisma.student.findUnique({
        where: {
            email,
        },
    });
    if (student != null) res.json(student);
    else res.sendStatus(401);
});
//get classes by student email
/**
 * returns array of classes if student have any
 * returns empty array if student doesn't have any classes
 * reurns status 404 if there is no such email
 */
router.get('/student_classes/:email', async (req, res) => {
    const email = req.params.email;
    if (!email) {
        res.sendStatus(406);
    }
    const result = await prisma.student.findUnique({
        where: {
            email,
        },
        select: {
            classes: {
                select: {
                    date: true,
                    subject_name: true,
                },
            },
        },
    });
    if (result === null) res.sendStatus(404);
    else res.json(result.classes || []);
});

export default router;
