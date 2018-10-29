const { Server } = require("ws");
const uuidv4 = require("uuid/v4");
const readline = require("readline");

const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
});

const port = process.env.PORT || 8080
const server = new Server({ port });
const workers = new Map();
const results = new Map();

console.log("==== MOBSTER-TFLITE ===")
console.log(`Started on port ${port}!`);
console.log("Type in \"start\" to start");
console.log("\nWaiting for workers...");

const onResult = uuid => (result) => {
    console.log(`Worker ${uuid} completed a task!`);

    const bucket = results.get(uuid) || [];
    bucket.push(result);

    results.set(uuid, bucket);
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

server.on("connection", (socket) => {
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
        workers.forEach((socket, uuid) => {
            const { readyState, OPEN } = socket;
            if (readyState === OPEN) {
                console.log(`Sending ${uuid}`);
                socket.send(`Hello ${uuid}`);
            } else {
                onDisconnect(uuid)();
            }
        });
    }
});