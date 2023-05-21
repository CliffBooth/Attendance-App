import { PrismaClient } from '@prisma/client';
import {encrypt} from './encrypt';

const prisma = new PrismaClient();

async function populate() {
    const professor = await prisma.professor.upsert({
        where: {
            email: 'test',
        },
        create: {
            email: 'test',
            password: (await encrypt('123'))
        },
        update: {},
    });
    console.log(professor);
    const student1 = await prisma.student.upsert({
        where: {
            phoneId: '1',
        },
        create: {
            phoneId: '1',
            firstName: 'student',
            secondName: 'first',
        },
        update: {},
    });
    const student2 = await prisma.student.upsert({
        where: {
            phoneId: '2',
        },
        create: {
            phoneId: '2',
            firstName: 'stduent',
            secondName: 'second',
        },
        update: {},
    });
    const student3 = await prisma.student.upsert({
        where: {
            phoneId: '3',
        },
        create: {
            phoneId: '3',
            firstName: 'student',
            secondName: 'third',
        },
        update: {},
    });
    console.log(student1, student2, student3);
    const class1 = await prisma.class.upsert({
        where: {
            id: 1,
        },
        create: {
            date: new Date().getTime(),
            subjectName: 'math',
            professor: {
                connect: {
                    id: professor.id
                }
            },
            students: {
                connect: [{ id: student1.id }, { id: student2.id }],
            },
        },
        update: {},
    });
    const class2 = await prisma.class.upsert({
        where: {
            id: 2,
        },
        create: {
            date: new Date().getTime(),
            subjectName: 'history',
            professorId: professor.id,
            students: {
                connect: [{ id: student2.id }, { id: student3.id }],
            },
        },
        update: {},
    });

    const class3 = await prisma.class.upsert({
        where: {
            id: 1,
        },
        create: {
            date: new Date().getTime(),
            subjectName: 'math',
            professor: {
                connect: {
                    id: professor.id
                }
            },
            students: {
                connect: [{ id: student1.id }, { id: student2.id }, {id: student3.id}],
            },
        },
        update: {},
    });

    const class4 = await prisma.class.upsert({
        where: {
            id: 1,
        },
        create: {
            date: new Date().getTime(),
            subjectName: 'math',
            professor: {
                connect: {
                    id: professor.id
                }
            },
            students: {
                connect: [{id: student3.id}],
            },
        },
        update: {},
    });

    const class5 = await prisma.class.upsert({
        where: {
            id: 1,
        },
        create: {
            date: new Date().getTime(),
            subjectName: 'math',
            professor: {
                connect: {
                    id: professor.id
                }
            },
            students: {
                connect: [{ id: student2.id }, {id: student3.id}],
            },
        },
        update: {},
    });
    
    //professor, who don't have any classes
    //what will getClasses endpoint return to professor who don't have any classes? null?
    //actually it should return empty array to professor, who don't have any classes and 
    //status 404 if there is no such professor_email
    const professor2 = await prisma.professor.upsert({
        where: {
            email: "test2"
        },
        create: {
            email: "test2",
            password: await encrypt('123')
        },
        update: {}
    })
    console.log(class1, class2, professor2);
}

async function deleteAll() {
    await prisma.student.deleteMany({})
    await prisma.professor.deleteMany({})
    await prisma.class.deleteMany({})
}

// populate().catch(err => console.error(err));
// deleteAll().catch(err => console.error(err))

async function test() {
    const classes = await prisma.class.findMany()
    const example = classes[0]
    console.log(new Date(example.date.toString()).getTime())

    const str = example.date.toString()
    console.log('str = ', str)
    const date = new Date(str)
    console.log('DATE = ', date)
    const str1 = Number('1684708483391')
    const date1 = new Date(str1)
    console.log('DATE1 = ', date1)

    // const time = new Date().getTime()
    // console.log('time in js: ', time, ' length: ', time.toString().length)
    // console.log('time coming from kotlin: ', example.date.toString(), ' length: ', example.date.toString().length)
}

test()