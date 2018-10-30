
const http = require("http");
const { Server } = require("ws");
const uuidv4 = require("uuid/v4");
const express = require("express");
const readline = require("readline");

const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
});

const port = process.env.PORT || 8080
const ip = process.env.IP || "localhost";

const app = express();
const server = http.createServer(app);

const wss = new Server({ server });
const workers = new Map();
const results = new Map();

const onResult = uuid => (result) => {
    const [id, ...probs] = result.split("\|"); 
    results.set(id, probs);
    
    console.log(`Worker ${uuid} completed task ${id}!`);
}

const onDisconnect = uuid => () => {
    console.log(`Worker ${uuid} disconnected`)

    if (workers.has(uuid)) {
        workers.delete(uuid);
    }
    console.log(`Total workers: ${countWorkers()}`);
}

const countWorkers = () => {
    let count = 0;
    for (const [uuid, socket] of workers.entries()) {
        const { readyState, OPEN } = socket;
        if (readyState !== OPEN) {
            console.log(`Worker ${uuid} state: ${readyState}`);
            onDisconnect(uuid)();
        } else {
            count++;
        }
    }
    return count;
}

app.use(express.static("public"));

wss.on("connection", (socket) => {
    const uuid = uuidv4();
    console.log(`Worker ${uuid} connected!`);

    socket.on("close", onDisconnect(uuid));
    socket.on("message", onResult(uuid));

    workers.set(uuid, socket);
    console.log(`Total workers: ${countWorkers()}`);
});

rl.on("line", (message) => {
    const command = message.trim().toLowerCase();

    if ("start" === command) {
        console.log("\nSTARTED!")
        workers.forEach((socket, uuid) => {
            const { readyState, OPEN } = socket;
            if (readyState === OPEN) {
                console.log("Sending...");
                socket.send(`123|http://${ip}:${port}/image.jpg`);
            } else {
                onDisconnect(uuid)();
            }
        });
    }
});

server.listen(port, () => {
    console.log("==== MOBSTER-TFLITE ===")
    console.log(`Started on port ${port}!`);
    console.log("Type in \"start\" to start");
    console.log("\nWaiting for workers...");
});