# LCCOMPILER

Note that specific instructions for each part can be found above by clicking on the part name.

# Marking #

The marking will be done using an automated test suite on a dice machine using Java 8 (1.8 runtime).
Please note that you are not allowed to modify the `Main.java` file which is the main entry point to the compiler.
A checksum on the file will be performed to ensure the file has not be tempered with.
Also make sure that the build script provided remains unchanged so that your project can be built on dice.
Furthermore, you may not use any external libraries.

# Setup #

## Building
In order to build the project you must have Ant installed. 
If you are using an IDE, then you can import the build file.
Otherwise, you can build the project from the commandline by typing:
```
$ ant build
```
This command outputs your compiler in a directory called `bin` within the project structure. Thereafter, you can run your compiler from the commandline by typing:
```
$ java -cp bin Main
```
The parameter `cp` instructs the Java Runtime to include the local directory `bin` when it looks for class files.

You can find a series of tests in the `tests` folder. To run the lexer on one of them, you can type:

```
$ java -cp bin Main -lexer tests/fibonacci.c
```


You can clean the `bin` directory by typing:
```
$ ant clean
```
This command effectively deletes the `bin` directory.
