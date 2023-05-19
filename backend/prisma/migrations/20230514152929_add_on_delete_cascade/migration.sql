-- DropForeignKey
ALTER TABLE "StudentInPredefinedClass" DROP CONSTRAINT "StudentInPredefinedClass_predefinedClassId_fkey";

-- AddForeignKey
ALTER TABLE "StudentInPredefinedClass" ADD CONSTRAINT "StudentInPredefinedClass_predefinedClassId_fkey" FOREIGN KEY ("predefinedClassId") REFERENCES "PredefinedClass"("id") ON DELETE CASCADE ON UPDATE CASCADE;
