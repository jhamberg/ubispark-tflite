const http = require("http");
const uuidv4 = require("uuid/v4");
const express = require("express");
const readline = require("readline");
const WebSocket = require("ws");
const Deque = require("denque");
const Lazy = require("lazy.js");

const UPDATE_BUFFER = "UPDATE_BUFFER";
const SUBMIT_RESULT = "SUBMIT_RESULT";

(async () => {
    const port = process.env.PORT || 8080
    const app = express();
    const server = http.createServer(app);
    const wss = new WebSocket.Server({ server });
    const terminal = readline.createInterface({ input: process.stdin });

    const results = new Map();
    const workers = new Deque();

    // Point here is to lazily evaluate the file processing pipeline to
    // significantly speed up the initialization phase. The resulting 
    // object is a promise by "accident", which is why we need to wrap 
    // the entire code in an anonymous async function.
    const listfile = await Lazy.readFile("listfile")
        .lines()
        .take(1000) // Modify this to control job size 
        .toArray();

    // Serve data chunks as static HTTP resources
    app.use(express.static("public"));

    const printTotalWorkers = () =>
        console.log(`Total workers: ${wss.clients.size}`);

    // Declarative repeat without memory allocation
    const times = (count, callback) =>
        Lazy.generate(callback, count).each(Lazy.noop);

    const createMessageHandler = worker => (message) => {
        const [type, value, ...rest] = message.split("\|");
        switch (type) {
            case UPDATE_BUFFER:
                console.log(`Worker ${worker.id} set buffer size: ${value}`);
                worker.taskBuffer = value;
                break;
            case SUBMIT_RESULT:
                console.log(`Worker ${worker.id} completed ${value}: ${rest}!`);
                results.set(value, rest);
                worker.activeTasks--;
                break;
        }
    }

    const runJob = () => new Promise((resolve) => {
        start = Date.now();
        workers.clear(); 
        wss.clients.forEach(x => workers.push(x));
        const tasks = listfile.slice(0);
        
        // Keep shifting workers from the dequeue in a loop
        setInterval(() => {
            if (results.size === listfile.length) {
                clearInterval(runJob);
                resolve();
            }

            const worker = workers.shift();

            if (worker != null && worker.readyState === WebSocket.OPEN) {
                const available = worker.taskBuffer - worker.activeTasks;
                times(available, () => {
                    const task = tasks.pop();
                    if (task) {
                        worker.send(task);
                        worker.activeTasks++;
                    }
                });
            }
            
            workers.push(worker);
        }, 0)
    });

    wss.on("connection", (worker) => {
        worker.id = uuidv4();
        worker.taskBuffer = 12;
        worker.activeTasks = 0;

        console.log(`Worker ${worker.id} connected!`);
        printTotalWorkers();
        
        // This allows workers to join in late
        workers.unshift(worker); 

        worker.on("message", createMessageHandler(worker));
        worker.on("close", () => {
            console.log(`Worker ${worker.id} disconnected`);
            printTotalWorkers();
        });

    });

    terminal.on("line", async (message) => {
        const command = message.trim().toLowerCase();
        if ("ready" === command) {
            const start = Date.now();
            await runJob();
            console.log(`Finished in ${Date.now() - start} ms!`);
        }
    });
    
    // Start the server
    server.listen(port, () => {
        console.log(`Loaded listfile with ${listfile.length} entries`);
        console.log(`Master server started on port ${port}!`);
        console.log("Type in \"ready\" to start");
        console.log("\nWaiting for workers...");
    });
})();
