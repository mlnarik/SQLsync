<?

//include 'SyncSettings.php';
include 'SQLCmds.php';



function addSyncTableColumns($tableName) {

	if (SyncSettings::DATABASE_TYPE == "PostgreSQL") {
	 
		$query = "ALTER TABLE $tableName ADD COLUMN ".
			"_lastchange timestamp with time zone NOT NULL DEFAULT clock_timestamp()";		
		sqlQuery($query);

		$query = "CREATE SEQUENCE {$tableName}_syncseq INCREMENT BY 100 START 100 OWNED BY $tableName.". SyncSettings::SYNC_ID_COL ."";		
		sqlQuery($query);

		$query = "ALTER TABLE $tableName ALTER COLUMN ". SyncSettings::SYNC_ID_COL ." SET DEFAULT nextval('{$tableName}_syncseq')";		
		sqlQuery($query);
		
		$query = "CREATE TRIGGER _SQLsync_update_$tableName ".
			"BEFORE UPDATE ON $tableName ".
			"FOR EACH ROW ".
			"EXECUTE PROCEDURE updateLastChange(); ";		
		sqlQuery($query);
		
		$query = "CREATE TRIGGER _SQLsync_delete_$tableName ".
		    "AFTER DELETE ON $tableName ".
		    "FOR EACH ROW ".
			"EXECUTE PROCEDURE logDelete(); ";
		sqlQuery($query);	
	}
	elseif (SyncSettings::DATABASE_TYPE == "MySQL")	{
	
		$query = "ALTER TABLE `$tableName` ADD `_lastchange` TIMESTAMP ".
			"ON UPDATE CURRENT_TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP";				
		sqlQuery($query);
		
		$query = "CREATE TRIGGER _SQLsync_delete_{$tableName} AFTER DELETE ".
			"ON $tableName FOR EACH ROW BEGIN ".
			"INSERT INTO __SQLsyncDeletedRows VALUES ('$tableName',OLD.". SyncSettings::SYNC_ID_COL .",OLD._timeInserted,NOW());".
			"END;";				
		sqlQuery($query); 
	}	
}

function addSyncManagement() {

	sqlQuery("DROP TABLE IF EXISTS __SQLsyncDeletedRows");
	sqlQuery("DROP TABLE IF EXISTS __SQLsyncLogs");
	sqlQuery("DROP TABLE IF EXISTS __SQLsyncDevices");
	sqlQuery("DROP FUNCTION IF EXISTS logdelete()");
	sqlQuery("DROP FUNCTION IF EXISTS updatelastchange()");
	
	$query = "CREATE TABLE __SQLsyncDevices (
	  androidid varchar(100) NOT NULL,
	  deviceid serial primary key NOT NULL
	);";
	sqlQuery($query);

	$query = "CREATE TABLE __SQLsyncDeletedRows (
	  tableName varchar(100) NOT NULL,
	  ". SyncSettings::SYNC_ID_COL ." int NOT NULL,
	  _timedeleted timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,	  
	  UNIQUE (tableName,". SyncSettings::SYNC_ID_COL .")
	);";
	sqlQuery($query);
	
	$query = "CREATE TABLE __SQLsyncLogs (
	  tableName varchar(100) NOT NULL,
	  ". SyncSettings::SYNC_ID_COL ." int NOT NULL,
	  lastchange timestamp with time zone NOT NULL,
	  values TEXT NOT NULL
	);";
	sqlQuery($query);

	if (SyncSettings::DATABASE_TYPE == "PostgreSQL") {
	
		$query = "CREATE OR REPLACE FUNCTION updateLastChange() RETURNS TRIGGER ".
			"LANGUAGE plpgsql AS $$".
			"BEGIN ".
			"NEW._lastChange=clock_timestamp(); ".
			"RETURN NEW; ".
			"END; $$;";
		sqlQuery($query);
		
		$query = "CREATE OR REPLACE FUNCTION logDelete() ".
			"RETURNS TRIGGER AS $$ ".
			"BEGIN ".
			"INSERT INTO __SQLsyncDeletedRows VALUES (TG_TABLE_NAME,OLD.". SyncSettings::SYNC_ID_COL ."); ". 
			"RETURN OLD;".
			"END; $$ LANGUAGE plpgsql;";
		sqlQuery($query);
	}
	
}



		
?>