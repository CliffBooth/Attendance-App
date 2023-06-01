import * as dotenv from 'dotenv'
import app from './app'
import prisma from './client';

dotenv.config()
export const PORT = parseInt(process.env.PORT ?? '8080');

app().listen(PORT, () => console.log(`started on port ${PORT}`));