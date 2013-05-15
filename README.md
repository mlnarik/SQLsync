SQLsync
=======

This library will help you use master-master database synchronization (replication) between PostgreSQL and Android's SQLite database.

##Features
- Fast,
- easy to use,
- automatic conflict resolution,
- minimal database usage restrictions,
- logs conflicts.

##Instalation

It is necessary to set up the library. If you don't follow described steps, it might not work correctly.

1. Install Android SDK, which also contains Eclipse, see http://developer.android.com/sdk/index.htm.
2. Install library by importing it. Click on Import in File menu. Select Android > Existing Android Code Into Workspace and click on Next then Browse and select folder with this library. Then click on Finish.
3. Create your own project by clicking on Android Application Project in File > New menu.
4. Right click on your project, click on Properties, choose Android, click on Add... in Library tab and the add SQLsync library.
5. Add "manifestmerger.enabled=true" into "project.properties" file that is part of your project.
6. Set your timezone for your Android emulator by clicking on Debug Configuration in Run menu. Then click on Target tab. In text field Additional Emulator Command Line Options enter your timezone, e.g. "-timezone Europe/Berlin".
7. Extend SyncDatabaseHelper class and adapt it to your own liking. See SampleDatabaseHelper class in SampleSQL how to use it.
8. Extend SyncProvider class. See SampleProvider for reference.
