# mobster-tflite

1) Download the [CORe50 128x128 dataset](https://vlomonaco.github.io/core50/index.html)
2) Extract in master/public/ (including the core50_128x128 directory)
3) Generate a listfile with `bash generate-listfile` in master/
4) Run the master with:
```
node master.js
```

5) Change `ENDPOINT_MASTER` in client/app/src/.../Constants.java to your IP
6) Start up the clients, make sure they show up on master
7) Type "ready" in master console to run the job

___

(C) 2018 - Jonatan Hamberg &lt;jonatan.hamberg@cs.helsinki.fi&gt;
