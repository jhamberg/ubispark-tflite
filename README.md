# Tensorflow Lite - based object detection executor: ubispark-tflite

This program was used to execute tasks on clusters of smartphones as part of the publication "Pervasive Data Science on the Edge" published in IEEE Pervasive Computing in 2019 and supported by Academy of Finland ( Project number 297741 ).

Full text (authors' copy) available at: https://researchportal.helsinki.fi/en/publications/pervasive-data-science-on-the-edge 

Official publication: https://ieeexplore.ieee.org/document/8915957

## Usage

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

## Standalone version

For comparison, there is a non-distributed (but threaded) Java implementation under /standalone.
You can build it using IntelliJ IDEA and Maven.

**NOTE:** Using a .pb instead of .tflite requires that colors are formatted in 0-1 range.

**NOTE:** TF handles threading, rolling your own threadpool might end up in deadlock. 

___

(C) 2018 - Jonatan Hamberg &lt;jonatan.hamberg@cs.helsinki.fi&gt;
