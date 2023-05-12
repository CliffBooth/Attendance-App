/*
  Warnings:

  - You are about to drop the `_PredefinedClassToStudentInPredefinedClass` table. If the table is not empty, all the data it contains will be lost.
  - Added the required column `predefinedClassId` to the `StudentInPredefinedClass` table without a default value. This is not possible if the table is not empty.

*/
-- DropForeignKey
ALTER TABLE "_PredefinedClassToStudentInPredefinedClass" DROP CONSTRAINT "_PredefinedClassToStudentInPredefinedClass_A_fkey";

-- DropForeignKey
ALTER TABLE "_PredefinedClassToStudentInPredefinedClass" DROP CONSTRAINT "_PredefinedClassToStudentInPredefinedClass_B_fkey";

-- AlterTable
ALTER TABLE "StudentInPredefinedClass" ADD COLUMN     "predefinedClassId" INTEGER NOT NULL;

-- DropTable
DROP TABLE "_PredefinedClassToStudentInPredefinedClass";

-- AddForeignKey
ALTER TABLE "StudentInPredefinedClass" ADD CONSTRAINT "StudentInPredefinedClass_predefinedClassId_fkey" FOREIGN KEY ("predefinedClassId") REFERENCES "PredefinedClass"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
