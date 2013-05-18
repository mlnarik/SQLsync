#SQLsync

This library will help you synchronize databases across all mobile devices that uses your application. Library provides fast master-master replication that synchronizes application's database on any Android mobile device with remote PostgreSQL database. Remote database serves as middleman, it's always available and helps with synchronization.

##Features
- fast
- easy to use
- automatic conflict resolution
- minimal database usage restrictions
- minimal hardware requirements
- logs conflicts

##Requirements
- custom server with PostgreSQL database (MySQL supported soon)
- knowledge about master-master replication limitations (see [Oracle article](http://docs.oracle.com/cd/B12037_01/server.101/b10732/repconfl.htm))

##Installation

These are necessary steps to set up the library. If you don't follow described steps, synchronization might not work correctly.

1. Install Android SDK, which also contains Eclipse. See [Android SDK](http://developer.android.com/sdk/index.html) (Note: Google will soon provide custom IDE).
2. Import this library to Eclipse. Click on Import in File menu. Select Android > Existing Android Code Into Workspace and click on Next then Browse and select folder with this library. Then click on Finish.
3. Create your own project by clicking on Android Application Project in File > New menu.
4. Right click on your project, click on Properties, choose Android, click on Add... in Library tab and then add SQLsync library.
5. Add "manifestmerger.enabled=true" into "project.properties" file that is part of your project.
6. Set your timezone for your Android emulator by clicking on Debug Configuration in Run menu. Then click on Target tab. In text field Additional Emulator Command Line Options enter your timezone, e.g. "-timezone Europe/Berlin".
7. Extend SyncDatabaseHelper class and adapt it to your own liking. See SampleDatabaseHelper class in SampleSQLsync how to use it.
8. Extend SyncProvider class. See SampleProvider for reference.
9. Upload webservice files from "SQLsync-webservice" folder to your server.
10. Create custom script for creating your own database structure, execute it and remove from server to avoid misuse. See file SampleCreateDatabase.php.
11. In file SyncSettings.php set accepted name and password.
12. Use SyncExecutor.createAccount(name, password, getApplicationContext()) to create synchronization account. Name and password will be send to the webservice on your remote server.
11. Use class SyncSettings in package "com.rathma.sqlsync.settings" to define tables for synchronization. Set URL address to webservice in field URL\_WEBSERVICE. Set AUTHORITY field to name of your project's package.
12. Set res/value.xml to name of your project's package.
