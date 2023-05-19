/*
  Warnings:

  - A unique constraint covering the columns `[subjectName,professorId]` on the table `PredefinedClass` will be added. If there are existing duplicate values, this will fail.

*/
-- CreateIndex
CREATE UNIQUE INDEX "PredefinedClass_subjectName_professorId_key" ON "PredefinedClass"("subjectName", "professorId");
