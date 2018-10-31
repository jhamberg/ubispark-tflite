const fs = require('fs')
const http = require("http");
const uuidv4 = require("uuid/v4");
const express = require("express");
const readline = require("readline");
const WebSocket = require("ws");
const Deque = require("denque");

const port = process.env.PORT || 8080
const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });
const terminal = readline.createInterface({ input: process.stdin });

const ip = process.env.IP || "localhost";
const results = new Map();
const workers = new Deque();
const listfile = fs
    .readFileSync("listfile")
    .toString()
    .split("\n");

// Serve images as static HTTP resources
app.use(express.static("public"));

const printTotalWorkers = () => 
    console.log(`Total workers: ${wss.clients.size}`);

wss.on("connection", (socket) => {
    const uuid = uuidv4();
    console.log(`Worker ${uuid} connected!`);
    printTotalWorkers()

    socket.on("close", () => {
        console.log(`Worker ${uuid} disconnected`);
        printTotalWorkers()
    });

    socket.on("message", (result) => {
        const [filepath, ...values] = result.split("\|"); 
        results.set(filepath, values);
        console.log(`Worker ${uuid} completed ${filepath}: ${values}!`);
        workers.push(socket);
    });
});

terminal.on("line", (message) => {
    const command = message.trim().toLowerCase();
    if ("ready" === command) {
        workers.clear();
        workers.push(...wss.clients);

        setInterval(() => {
            const worker = workers.shift();
            if (worker && worker.readyState === WebSocket.OPEN) {
                const filepath = listfile.pop();
                worker.send(`http://${ip}:${port}/${filepath}`);
            }
        }, 0);
    }
});

server.listen(port, () => {
    console.log(`Loaded listfile with ${listfile.length} entries`);
    console.log(`Master server started on port ${port}!`);
    console.log("Type in \"ready\" to start");
    console.log("\nWaiting for workers...");
});