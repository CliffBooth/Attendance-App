import * as dotenv from "dotenv"
import express from "express"

dotenv.config()

if (!process.env.PORT) {
    console.error("No PORT environment variable!")
    process.exit(1)
}

const PORT = parseInt(process.env.PORT)
const app = express()

app.get('/start', (req, res) => {
    console.log(`/start request from ip: ${req.ip}`)
    res.send("Hello!").status(200)
})


app.listen(PORT, () => console.log(`started on port ${PORT}`))