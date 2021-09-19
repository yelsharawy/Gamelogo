cd classes
del *.class
cd ..
javac -classpath "C:\Program Files\NetLogo 6.1.1\app\netlogo-6.1.1.jar;lib\system-hook-3.2.jar" -d classes gamelogo/*.java
jar cvfm gamelogo.jar manifest.txt -C classes .
PAUSE
rem ";lib\jna-4.5.2.jar;lib\jna-platform-4.5.2.jar"