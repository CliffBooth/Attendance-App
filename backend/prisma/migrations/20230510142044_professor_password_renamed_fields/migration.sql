/*
  Warnings:

  - Added the required column `password` to the `Professor` table without a default value. This is not possible if the table is not empty.

*/
-- DropForeignKey
ALTER TABLE "Class" DROP CONSTRAINT "Class_professor_id_fkey";

-- AlterTable
ALTER TABLE "Professor" ADD COLUMN     "password" TEXT NOT NULL;

-- AddForeignKey
ALTER TABLE "Class" ADD CONSTRAINT "Class_professor_id_fkey" FOREIGN KEY ("professor_id") REFERENCES "Professor"("id") ON DELETE CASCADE ON UPDATE CASCADE;
