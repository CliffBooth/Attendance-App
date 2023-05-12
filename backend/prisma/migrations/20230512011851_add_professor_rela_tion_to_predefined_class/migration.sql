/*
  Warnings:

  - Added the required column `professorId` to the `PredefinedClass` table without a default value. This is not possible if the table is not empty.

*/
-- AlterTable
ALTER TABLE "PredefinedClass" ADD COLUMN     "professorId" INTEGER NOT NULL;

-- AddForeignKey
ALTER TABLE "PredefinedClass" ADD CONSTRAINT "PredefinedClass_professorId_fkey" FOREIGN KEY ("professorId") REFERENCES "Professor"("id") ON DELETE CASCADE ON UPDATE CASCADE;
