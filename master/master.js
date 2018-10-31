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

const results = new Map();
const workers = new Deque();
const listfile = fs
    .readFileSync("listfile")
    .toString()
    .split("\n")
    .slice(0, 1000);

let start;

// Serve images as static HTTP resources
app.use(express.static("public"));

const printTotalWorkers = () => 
    console.log(`Total workers: ${wss.clients.size}`);

const job = () => new Promise((resolve) => {
    start = Date.now();
    workers.clear(); 
    wss.clients.forEach(x => workers.push(x));
    const tasks = listfile.slice(0);
    
    // Keep shifting workers from the dequeue in a loop
    setInterval(() => {
        const worker = workers.shift();
        if (worker && worker.readyState === WebSocket.OPEN) {
            const task = tasks.pop();
            if (task) {
                worker.send(task);
                workers.push(worker);
            } else {
                clearInterval(job);
                resolve();
            }
        }
    }, 0)
});

wss.on("connection", (socket) => {
    const uuid = uuidv4();
    console.log(`Worker ${uuid} connected!`);
    printTotalWorkers();

    // Allow late join
    workers.unshift(socket); 

    socket.on("close", () => {
        console.log(`Worker ${uuid} disconnected`);
        printTotalWorkers();
    });

    socket.on("message", (result) => {
        const [filepath, ...values] = result.split("\|"); 
        results.set(filepath, values);
        console.log(`Worker ${uuid} completed ${filepath}: ${values}!`);
        if (results.size === listfile.length) {
            console.log(`Done in ${Date.now() - start} ms!`);
        }
    });
});

terminal.on("line", async (message) => {
    const command = message.trim().toLowerCase();
    if ("ready" === command) {
        await job();
        console.log("Sent all tasks!");
    }
});


server.listen(port, () => {
    console.log(`Loaded listfile with ${listfile.length} entries`);
    console.log(`Master server started on port ${port}!`);
    console.log("Type in \"ready\" to start");
    console.log("\nWaiting for workers...");
});