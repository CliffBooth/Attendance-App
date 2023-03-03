import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function populate() {
    const professor = await prisma.professor.upsert({
        where: {
            email: 'test',
        },
        create: {
            email: 'test',
        },
        update: {},
    });
    console.log(professor);
    const student1 = await prisma.student.upsert({
        where: {
            email: '1',
        },
        create: {
            email: '1',
            first_name: 'student',
            second_name: 'first',
        },
        update: {},
    });
    const student2 = await prisma.student.upsert({
        where: {
            email: '2',
        },
        create: {
            email: '2',
            first_name: 'stduent',
            second_name: 'second',
        },
        update: {},
    });
    const student3 = await prisma.student.upsert({
        where: {
            email: '3',
        },
        create: {
            email: '3',
            first_name: 'student',
            second_name: 'third',
        },
        update: {},
    });
    console.log(student1, student2, student3);
    const class1 = await prisma.class.upsert({
        where: {
            id: 1,
        },
        create: {
            subject_name: 'math',
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
            subject_name: 'history',
            professor_id: professor.id,
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
            subject_name: 'math',
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
            subject_name: 'math',
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
            subject_name: 'math',
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
            email: "test2"
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

populate().catch(err => console.error(err));
// deleteAll().catch(err => console.error(err))
