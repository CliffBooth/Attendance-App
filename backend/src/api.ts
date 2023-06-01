import express from 'express';
import { PrismaClient, Professor, Student } from '@prisma/client';
import { Request } from 'express';
import * as jwt from 'jsonwebtoken';
import { encrypt, compare } from './encrypt';
import * as dotenv from 'dotenv'

export default function router(prisma: PrismaClient) {
const router = express.Router();
    
dotenv.config()
const SECRET = process.env.SECRET;
if (!SECRET) {
    throw new Error('secret key is undefined!');
}

const verifyToken = (
    req: express.Request,
    res: express.Response,
    next: express.NextFunction
) => {
    const authHeader = req.headers.authorization;
    if (!authHeader) {
        res.status(401).send('no authorization header');
        return;
    }
    const token = authHeader.split(' ')[1];
    try {
        const user = jwt.verify(token, SECRET) as any;
        (req as any).jwtData = { user };
        next();
    } catch (err) {
        console.log(err);
        res.status(401).send('invalid token');
        return;
    }
};

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
    .get(verifyToken, async (req, res) => {
        console.log('after verify');
        const email = req.params.email;
        const user = (req as any).jwtData.user as Professor;
        if (user.email !== email) {
            console.log(
                "jwt doesn't correspond to the url! user.email, email ",
                user.email,
                email
            );
            res.sendStatus(401);
            return;
        }

        const result = await prisma.professor.findUnique({
            where: {
                email,
            },
            include: {
                classes: {
                    select: {
                        students: {
                            select: {
                                phoneId: true,
                                firstName: true,
                                secondName: true,
                            },
                        },
                        subjectName: true,
                        date: true,
                    },
                },
            },
        });

        if (result === null){
            res.sendStatus(404);
            return;
        }
        const toReturn = result?.classes.map(cl => ({...cl, date: Number(cl.date.toString())}))
        res.json(toReturn);

    })
    //save a new class object with professor's email
    /**
     * empty string date => defualt date.
     * student.email == null => create new student
     */
    .post(
        verifyToken,
        async (req: Request<{ email: string }, {}, RequestClass>, res) => {
            console.log('POST_SESSION');
            const prof_email = req.params.email;
            const user = (req as any).jwtData.user as Professor;
            if (user.email !== prof_email) {
                console.log(
                    "jwt doesn't correspond to the url! user.email, email ",
                    user.email,
                    prof_email
                );
                res.status(401).send('invalid access token');
                return;
            }
            //check if professor with such email exists:
            const prof = await prisma.professor.findUnique({
                where: {
                    email: prof_email,
                },
            });

            if (prof === null) {
                console.log('prof === null');
                res.status(404).send('wrong email');
                return;
            }

            const data = req.body;

            console.log(`POST /professor_classes/${prof_email}`, data);

            if (data.students === undefined || data.subjectName === undefined) {
                console.log(data.students, data.subjectName);
                res.status(406).send('wrong data format');
                return;
            }

            console.log('running');

            const students: Student[] = [];

            for (let s of data.students) {
                if (s.phoneId != undefined && s.phoneId != null) {
                    console.log('s.email != undefined && s.email != null');
                    try {
                        const student = await prisma.student.findUnique({
                            where: {
                                phoneId: s.phoneId,
                            },
                        });
                        if (student != null) {
                            students.push(student);
                        } else {
                            console.log(
                                `non existant student id (email): ${s.phoneId}`
                            );
                        }
                    } catch (error) {
                        console.log('database query error ', error);
                        res.status(500).send('database query error');
                        return;
                    }
                } else if (
                    s.firstName != undefined &&
                    s.secondName != undefined
                ) {
                    console.log(
                        's.firstName != undefined && s.secondName != undefined'
                    );
                    try {
                        const student = await prisma.student.create({
                            data: {
                                firstName: s.firstName,
                                secondName: s.secondName,
                            },
                        });
                        students.push(student);
                    } catch (error) {
                        console.log('database query error ', error);
                        res.status(500).send('database query error');
                        return;
                    }
                }
            }

            // console.log(students);

            const result = await prisma.class.create({
                data: {
                    date: data.date,
                    subjectName: data.subjectName,
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

            const toSend = {
                ...result,
                date: Number(result.date.toString())
            }

            res.json(toSend);
        }
    );

interface RequestClass {
    date: number,
    subjectName: string;
    students: RequestStudent[];
}

interface RequestStudent {
    phoneId?: string;
    firstName?: string;
    secondName?: string;
}

//create professor (takes email)
/**
 * returns:
 *  200 if created
 *  406 if incorrect body
 *  409 if email already exists
 */
router.post('/signup_professor', async (req, res) => {
    const email = req.body.email;
    const password = req.body.password;

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

    const hashedPassword = await encrypt(password);

    const prof = await prisma.professor.create({
        data: {
            email: email,
            password: hashedPassword,
        },
    });

    console.log(`=====DELETE==== IN SIGNUP prof = `, prof)

    const token = jwt.sign(prof, SECRET);
    res.json({
        token,
    }); //TODO: jwt!!
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
        if (!data.phoneId || !data.firstName || !data.secondName) {
            res.sendStatus(406);
            return;
        }

        const existing = await prisma.student.findUnique({
            where: {
                phoneId: data.phoneId,
            },
        });

        if (existing != null) {
            res.sendStatus(409);
            return;
        }

        const student = await prisma.student.create({
            data: {
                phoneId: data.phoneId,
                firstName: data.firstName,
                secondName: data.secondName,
            },
        });

        // const token = jwt.sign(student, SECRET);
        // res.json({ student, token });
        res.json(student);
    }
);

interface BodyStudentSignup {
    phoneId: string;
    firstName: string;
    secondName: string;
}
//login: check if professor with such email exists
/**
 * returns:
 *  200 if logged in
 *  406 if body doesn't contain "email" or "password"
 *  401 wrong password
 *  409 no such email
 */
router.post('/login_professor', async (req, res) => {
    const email = req.body.email;
    const password = req.body.password;

    if (!email || !password) {
        res.status(406).send('no email or password');
        return;
    }

    const professor = await prisma.professor.findUnique({
        where: {
            email,
        },
    });

    if (professor != null) {
        if (!(await compare(password, professor?.password))) {
            res.status(401).send('wrong password');
            return;
        }
        const token = jwt.sign(professor, SECRET);
        res.json({
            token,
        });
        // res.sendStatus(200);
    } else res.status(409).send('wrong emial');
});
//login: check if student with such email exists
/**
 * returns:
 *  200 if logged in
 *   406 if body doesn't contain "email"
 *  401 if no such email
 */
router.post('/login_student', async (req, res) => {
    const phoneId = req.body.phoneId;
    if (!phoneId) {
        res.sendStatus(406);
        return;
    }
    const student = await prisma.student.findUnique({
        where: {
            phoneId,
        },
    });

    if (student != null) {
        // const token = jwt.sign(student, SECRET);
        res.json(student);
    } else res.sendStatus(401);
});
//get classes by student email
/**
 * returns array of classes if student have any
 * returns empty array if student doesn't have any classes
 * reurns status 404 if there is no such email
 */
router.get('/student_classes/:phoneId', async (req, res) => {
    const phoneId = req.params.phoneId;
    if (!phoneId) {
        res.sendStatus(406);
    }

    // const user = (req as any).jwtData.user as Student;
    // if (user.phoneId !== phoneId) {
    //     console.log(
    //         "jwt doesn't correspond to the url! user.phoneId, phoneId ",
    //         user.phoneId,
    //         phoneId
    //     );
    //     res.sendStatus(401);
    //     return;
    // }

    const result = await prisma.student.findUnique({
        where: {
            phoneId,
        },
        select: {
            classes: {
                select: {
                    date: true,
                    subjectName: true,
                },
            },
        },
    });
    if (result === null) {
        res.sendStatus(404);
        return;
    }
    const toSend = result.classes.map(cl => ({...cl, date: Number(cl.date.toString())}))
    res.json(toSend || []);
});

/**
 * 406 - wrong input
 */

interface PostPredefined {
    subjectName: string;
    method: string;
}

router.post(
    '/predefinedClasses',
    verifyToken,
    async (req: Request<{}, {}, PostPredefined>, res) => {
        const user = (req as any).jwtData.user as Professor;

        const subjectName = req.body.subjectName;
        const method = req.body.method;
        if (!subjectName || !method) {
            res.status(406).send('wrong data');
            return;
        }

        try {
            const result = await prisma.predefinedClass.create({
                data: {
                    subjectName: subjectName,
                    method: {
                        connectOrCreate: {
                            where: {
                                name: method,
                            },
                            create: {
                                name: method,
                            },
                        },
                    },
                    professor: {
                        connect: {
                            id: user.id,
                        },
                    },
                },
            });
            res.json(result);
        } catch (err) {
            console.log(err);
            res.status(500).send('database error');
        }
    }
);

router.get('/predefinedClasses', verifyToken, async (req, res) => {
    const user = (req as any).jwtData.user as Professor;
    try {
        const result = await prisma.predefinedClass.findMany({
            where: {
                professorId: user.id,
            },
            include: {
                method: {
                    select: {
                        name: true,
                    },
                },
                students: {
                    select: {
                        firstName: true,
                        secondName: true,
                    },
                },
            },
        });
        const toSend = result.map(r => ({...r, updatedAt: new Date(r.updatedAt).getTime()}))
        res.json(toSend);
    } catch (err) {
        console.log(err);
        res.status(500).send('database error');
    }
});

interface UpdatePredefined {
    classId: number;
    method: string;
    subjectName: string;
    studentList: {
        firstName: string;
        secondName: string;
    }[];
}

/**
 * don't have to send predefined class id
 */
router.put(
    '/predefinedClasses',
    verifyToken,
    async (req: Request<{}, {}, UpdatePredefined>, res) => {
        const user = (req as any).jwtData.user as Professor;

        const { method, subjectName, studentList } = req.body;

        if (!method || !subjectName || !studentList) {
            res.status(406).send('wrong input');
            return;
        }

        try {
            const deleted = await prisma.studentInPredefinedClass.deleteMany({
                where: {
                    predefinedClass: {
                        subjectName: subjectName,
                        professorId: user.id,
                    }
                },
            });

            await prisma.predefinedClass.upsert({
                where: {
                    subjectName_professorId: {subjectName, professorId: user.id}
                },
                update:{},
                create: {
                    subjectName: subjectName,
                    method: {
                        connectOrCreate: {
                            where: {
                                name: method,
                            },
                            create: {
                                name: method,
                            },
                        },
                    },
                    professor: {
                        connect: {
                            id: user.id,
                        },
                    },
                }
            })

            const result = await prisma.predefinedClass.update({
                where: {
                    subjectName_professorId: {subjectName, professorId: user.id}
                },
                data: {
                    method: {
                        connectOrCreate: {
                            where: {
                                name: method,
                            },
                            create: {
                                name: method,
                            },
                        },
                    },
                    subjectName,
                    students: {
                        create: studentList.map(s => ({
                            firstName: s.firstName,
                            secondName: s.secondName,
                        })),
                    },
                },
                include: {
                    students: true
                }
            });
            const toSend = {...result, updatedAt: new Date(result.updatedAt).getTime()}
            res.json(toSend);
        } catch (err) {
            console.log(err);
            res.status(500).send('database error');
        }
    }
);

router.delete('/predefinedClasses', verifyToken, async (req, res) => {
    const id = req.body.id;
    if (!id) {
        res.status(406).send('wrong data');
    }

    try {
        const result = await prisma.predefinedClass.delete({
            where: {
                id,
            },
        });
        res.sendStatus(200);
    } catch (err) {
        console.log(err);
        res.status(500).send('database error');
    }
});

interface PostAttendanceBody {
    subjectName: string;
}

/**
 * return type:
 * {
    listOfStudents: [], //gathered from all classes + predefined if exists
    dateToStudents: {
        [date: string]: {
            firstName: string,
            secondName: string,
        }
    }[]
 * }
 */
// router.post('/attendance', verifyToken, async (req, res) => {
//     const body = req.body as PostAttendanceBody;

//     const user = (req as any).jwtData.user as Professor;

//     const fullListOfStudents: Student[] = [];
//     const dateToStudents: { [date: string]: Student[] }[] = [];

//     try {
//         const classes = await prisma.class.findMany({
//             where: {
//                 subjectName: body.subjectName,
//             },
//             include: {
//                 students: true,
//             },
//         });

//         classes.forEach(cl => {
//             const students = cl.students;
//             students.forEach(st => {
//                 if (st.phoneId) {
//                     if (!fullListOfStudents.find(s => s.phoneId === st.phoneId)) {
//                         fullListOfStudents.push(st);
//                     }
//                 } else {
//                     if (
//                         !fullListOfStudents.find(
//                             s =>
//                                 s.firstName.toLocaleLowerCase() ===
//                                     st.firstName.toLocaleLowerCase() &&
//                                 s.secondName.toLowerCase() ===
//                                     st.secondName.toLocaleLowerCase()
//                         )
//                     ) {
//                         fullListOfStudents.push(st);
//                     }
//                 }
//             });
    
//             dateToStudents.push({ [cl.date.toString()]: cl.students });
//         });
        
//     } catch (err) {
//         console.log(err);
//         res.status(500).send('database error');
//         return;
//     }

//     console.log('after first try')
//     console.log(user)

//     try {
//         const predefinedClass = await prisma.predefinedClass.findFirst({
//             where: {
//                 professorId: user.id,
//                 subjectName: body.subjectName,
//             },
//             include: {
//                 students: true,
//             },
//         });
//         if (predefinedClass)
//             predefinedClass?.students.forEach(st => {
//                 if (
//                     !fullListOfStudents.find(
//                         s =>
//                             s.firstName.toLowerCase() ===
//                                 st.firstName.toLowerCase() &&
//                             s.secondName.toLowerCase() ===
//                                 st.secondName.toLowerCase()
//                     )
//                 ) {
//                     fullListOfStudents.push({ ...st, phoneId: null });
//                 }
//             });
//     } catch (err) {
//         console.log(err);
//         res.status(500).send('database error');
//         return;
//     }

//     res.json({
//         listOfStudents: fullListOfStudents,
//         dateToStudents: dateToStudents,
//     });
// });

    return router;
}

