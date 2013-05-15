#SQLsync

This library will help your Android application to synchronize and replicate your local Android SQLite database with remote PostgreSQL database.

##Features
- fast
- easy to use
- automatic conflict resolution
- minimal database usage restrictions
- minimal hardware requirements
- logs conflicts

##Requirements
- custom server with PostgreSQL database (MySQL supported soon)
- knowledge about master-master replication limitations

##Instalation

These are necessary steps to set up the library. If you don't follow described steps, synchronization might not work correctly (or at all).

1. Install Android SDK, which also contains Eclipse. See http://developer.android.com/sdk/index.htm.
2. Import this library to Eclipse. Click on Import in File menu. Select Android > Existing Android Code Into Workspace and click on Next then Browse and select folder with this library. Then click on Finish.
3. Create your own project by clicking on Android Application Project in File > New menu.
4. Right click on your project, click on Properties, choose Android, click on Add... in Library tab and the add SQLsync library.
5. Add "manifestmerger.enabled=true" into "project.properties" file that is part of your project.
6. Set your timezone for your Android emulator by clicking on Debug Configuration in Run menu. Then click on Target tab. In text field Additional Emulator Command Line Options enter your timezone, e.g. "-timezone Europe/Berlin".
7. Extend SyncDatabaseHelper class and adapt it to your own liking. See SampleDatabaseHelper class in SampleSQL how to use it.
8. Extend SyncProvider class. See SampleProvider for reference.
9. Upload webservice files from "SQLsync-webservice" folder to your server.
10. Use SyncExecutor.createAccount(name, password, getApplicationContext()) to create synchronization account. Name and password will be send to webservice on your remote server.
11. 
