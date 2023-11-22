This repository reproduces a regression seen with JDK 21.0.1 
compared to JDK < 21 and 21.0 (21+35).

How to reproduce

Set a JDK which you would like to test and run:

    ./gradlew run

If it works fine, there is a simple output of 


    Successfully validated document with 21


If it fails, there is similar output to

    Exception in thread "main" java.lang.IllegalStateException: Not valid for 21.0.1
    Validate returned: false
    HasNext: true, validate: false
            at org.dstadler.poi.reproduce.Reproduce.main(Reproduce.java:105)


This was reported at https://bugs.openjdk.org/browse/JDK-8320597 to see if 
this is caused by a change in the JDK which should be reverted/fixed. 
