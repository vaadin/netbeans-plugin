Setting up project to develop it in NetBeans
=========
Download target NB distribution. Depending on the git branch and desired binaries one could use:
1. https://netbeans.org/downloads/ contains installer for any NB version.
2. nb7.3.1 branch requires NB 7.3.1 binaries. See above for installer and https://netbeans.org/downloads/zip.html for binaries in archive.
3. master branch  requires 7.4 latest nightly binaries (7.4 beta doesn't fit). See first item for installer and http://bits.netbeans.org/dev/nightly/latest/zip/ for binaries in archive.
In both cases distribution must contain at least javaee cluster.

Start NetBeans.
-------------
Open netbeans plugin project. Now one can build project and create nbm file for plugin distribution via NetBeans.

Build project in headless environment.
-------------
To be able to build project via command line one needs to start NB at least once and open plugin project in UI. These actions set properties in the project that allows to find NB platform on the filesystem. Once that is done UI is not required anymore and project could be built via command line. Just run "ant" inside project's folder to build sources and "ant nbm" to get nbm plugin file.

Notes about requirement for initial NB run.
-------------
Initial NB start creates environment specific property file. It's path is nbproject/private/platform-private.properties. It contains property "user.properties.file" with path to the build.properties. The latter file is NB platform installation specific property file. It contains properties with available platforms, paths to those platforms and active platform (current platform to build against of). Having the latter file with correct settings and creating "nbproject/private/platform-private.properties" with "user.properties.file" set to its path could help to avoid initial NB run with project opening.
