# Multitailer

## Description

A multi­tailer for continuously updated logs which monitors the log files and then emits the valid lines to
standard output and the invalid lines to standard error with some amount of re­ordering to try 
and give a coherent single timeline of log messages.

### Getting Started

Requires gradle, python, JDK 1.8

### Build

`gradle build`

### Running the program

Execute with command <br>
`java -jar build/libs/multitailer.jar -D /path/to/logdir -T maxwait -B`

Arguments description
* -D : (required) Directory path containing the log files
* -T : (optional) maxwait in milliseconds for buffering input records
* -B : (optional) Pass this argument if the program should process the log files from beginning

### Script to generate input logs to test

*TODO: Make them more customizable*

Option 1:
```
cd tests
python testpython.py
```

Option 2: (Continously dump multiple file to another file in input directory)
```
cd tests
python testpython.py (Exit with ctrl+c)
python testpython_dump.py
```

Open another terminal to execute the program after starting logs generation)
```
cd $PROJECT_ROOT
java -jar build/libs/multitailer.jar -D tests/input -B
```

### Output

The desired solution emit the valid lines to standard output and the invalid lines to standard error can be easily fixed in code. However, the current implementation stores them to `output.log` and `error.log` files for easier reading.

### Design of the solution

* Each input file is read(tailed) using `Tailer` class provided in `org.apache.commons.io.input.Tailer`.
* `TailerListener` handles processing of each line from log and puts them into shared buffer `LinkedBlockingQueue`.
* Output thread drains the shared buffer periodically and splits into `output` and `error` logs.

### Further tasks

* If the log files are to frequently writing and rotating, `Tailer` doesn't seem to handle it well.
* Create a watcher service to monitor directory for new log files created so they too can be monitored. Current implementation assumes that files are already being generated in directory to be tailed.
* Make test scripts more customizable.
* Better test cases.
* Performance optimizations if possible.
