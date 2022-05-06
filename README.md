# Welcome

This is a plugin for [Automate-Everything](https://github.com/tomaszbabiuk/automate-everything). It provides a support for 1-Wire devices (DS2408, DS18B20, DS2480B, DS2402)

# Directory setup
```bash
md work
cd work
git clone https://github.com/tomaszbabiuk/automate-everything.git
git clone https://github.com/tomaszbabiuk/aeplugin-onewire.git
```

# Automate-Everything frontend installation
```bash
cd automate-everything
cd ae-frontent
npm install
npm run build
```
After running this commands, the website is installed in the 'automate-everything/output/web' folder

# Automate-Everything backend installation
```bash
cd automate-everyhing
gradlew.bat assembleBackend
```
After running this task, the backend application is installed in 'automate-everything/output/bin' folder

# Plugin installation
```bash
cd aeplugin-onewire
gradlew.bat buildForAutomateEverything
```
After running this task, the plugin is installed in 'automate-everything/output/plugins' folder 

# Running backend
```bash
cd output
java -jar output/bin/ae-backend-all.jar
```
After running the server, go to http://localhost/plugins/hardware and enable "One-wire" plugin

# Debugging
Run in IntelliJ Idea. Debug as "JAR application". 
Path to JAR:'automate-everything/output/bin/ae-backend-all.jar'
Working directory 'automate-everything/output'
Search sources using module's classpath: 'automate-everything.ae-backend.main'
Before lunch:
 - run gradle task: aeplugin-onewire:buildForAutomateEverything
 - run gradle task: automate-everything.ae-backend:assembleBackend