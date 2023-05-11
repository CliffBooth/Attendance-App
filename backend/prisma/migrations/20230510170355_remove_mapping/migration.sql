/*
  Warnings:

  - You are about to drop the column `professor_id` on the `Class` table. All the data in the column will be lost.
  - You are about to drop the column `subject_name` on the `Class` table. All the data in the column will be lost.
  - You are about to drop the column `email` on the `Student` table. All the data in the column will be lost.
  - You are about to drop the column `first_name` on the `Student` table. All the data in the column will be lost.
  - You are about to drop the column `second_name` on the `Student` table. All the data in the column will be lost.
  - A unique constraint covering the columns `[phoneId]` on the table `Student` will be added. If there are existing duplicate values, this will fail.
  - Added the required column `professorId` to the `Class` table without a default value. This is not possible if the table is not empty.
  - Added the required column `subjectName` to the `Class` table without a default value. This is not possible if the table is not empty.
  - Added the required column `firstName` to the `Student` table without a default value. This is not possible if the table is not empty.
  - Added the required column `secondName` to the `Student` table without a default value. This is not possible if the table is not empty.

*/
-- DropForeignKey
ALTER TABLE "Class" DROP CONSTRAINT "Class_professor_id_fkey";

-- DropIndex
DROP INDEX "Student_email_key";

-- AlterTable
ALTER TABLE "Class" DROP COLUMN "professor_id",
DROP COLUMN "subject_name",
ADD COLUMN     "professorId" INTEGER NOT NULL,
ADD COLUMN     "subjectName" TEXT NOT NULL;

-- AlterTable
ALTER TABLE "Student" DROP COLUMN "email",
DROP COLUMN "first_name",
DROP COLUMN "second_name",
ADD COLUMN     "firstName" TEXT NOT NULL,
ADD COLUMN     "phoneId" TEXT,
ADD COLUMN     "secondName" TEXT NOT NULL;

-- CreateIndex
CREATE UNIQUE INDEX "Student_phoneId_key" ON "Student"("phoneId");

-- AddForeignKey
ALTER TABLE "Class" ADD CONSTRAINT "Class_professorId_fkey" FOREIGN KEY ("professorId") REFERENCES "Professor"("id") ON DELETE CASCADE ON UPDATE CASCADE;
