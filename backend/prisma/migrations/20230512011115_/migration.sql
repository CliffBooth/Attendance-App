/*
  Warnings:

  - A unique constraint covering the columns `[name]` on the table `CheckMethods` will be added. If there are existing duplicate values, this will fail.

*/
-- CreateIndex
CREATE UNIQUE INDEX "CheckMethods_name_key" ON "CheckMethods"("name");
