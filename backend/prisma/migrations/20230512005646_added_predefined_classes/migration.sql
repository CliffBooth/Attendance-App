-- CreateTable
CREATE TABLE "StudentInPredefinedClass" (
    "id" SERIAL NOT NULL,
    "firstName" TEXT NOT NULL,
    "secondName" TEXT NOT NULL,

    CONSTRAINT "StudentInPredefinedClass_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "CheckMethods" (
    "id" SERIAL NOT NULL,
    "name" TEXT NOT NULL,

    CONSTRAINT "CheckMethods_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "PredefinedClass" (
    "id" SERIAL NOT NULL,
    "subjectName" TEXT NOT NULL,
    "methodId" INTEGER NOT NULL,

    CONSTRAINT "PredefinedClass_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "_PredefinedClassToStudentInPredefinedClass" (
    "A" INTEGER NOT NULL,
    "B" INTEGER NOT NULL
);

-- CreateIndex
CREATE UNIQUE INDEX "_PredefinedClassToStudentInPredefinedClass_AB_unique" ON "_PredefinedClassToStudentInPredefinedClass"("A", "B");

-- CreateIndex
CREATE INDEX "_PredefinedClassToStudentInPredefinedClass_B_index" ON "_PredefinedClassToStudentInPredefinedClass"("B");

-- AddForeignKey
ALTER TABLE "PredefinedClass" ADD CONSTRAINT "PredefinedClass_methodId_fkey" FOREIGN KEY ("methodId") REFERENCES "CheckMethods"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "_PredefinedClassToStudentInPredefinedClass" ADD CONSTRAINT "_PredefinedClassToStudentInPredefinedClass_A_fkey" FOREIGN KEY ("A") REFERENCES "PredefinedClass"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "_PredefinedClassToStudentInPredefinedClass" ADD CONSTRAINT "_PredefinedClassToStudentInPredefinedClass_B_fkey" FOREIGN KEY ("B") REFERENCES "StudentInPredefinedClass"("id") ON DELETE CASCADE ON UPDATE CASCADE;
