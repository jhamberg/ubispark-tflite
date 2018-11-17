# mobster-tflite

1) Download an image dataset, like the [CORe50](https://vlomonaco.github.io/core50/index.html) or [ILSVRC](http://image-net.org/challenges/LSVRC/)
2) Extract the files to a folder
3) Use the `preprocess` shell script to randomly select N files for classification:
```bash
$ preprocess <folder> <N>
```
4) Install the dependencies for master node using [npm](https://www.npmjs.com/)
```bash
$ cd master && npm install
```
5) Run the master node with [Node.js](https://nodejs.org):
```
node master.js
```

6) Build the client in [Android Studio](https://developer.android.com/studio/) 
7) Install and open the client on worker devices, enter the master `ip:port` and press START
8) Type "ready" in the master console to start the inference

___

(C) 2018 - Jonatan Hamberg &lt;jonatan.hamberg@cs.helsinki.fi&gt;
