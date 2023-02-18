/*
  Warnings:

  - Added the required column `subject_name` to the `Class` table without a default value. This is not possible if the table is not empty.

*/
-- AlterTable
ALTER TABLE "Class" ADD COLUMN     "subject_name" TEXT NOT NULL;
