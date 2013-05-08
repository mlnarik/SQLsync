<?

include 'Common.php';

@ini_set('default_charset', 'utf-8');
header('Content-Type: text/html; charset=utf-8');	

sqlConnect();	

// Removes BOM from UTF-8 (if there is)
if(substr($_POST["sync"], 0, 3) == pack("CCC", 0xEF, 0xBB, 0xBF)) {
    $_POST["sync"] = substr($_POST["sync"], 3);
}

$syncData = json_decode($_POST["sync"]);
if (is_null($syncData))
{
	echo "Error: JSON object is invalid, JSON error no: " . json_last_error() . "\n";
	echo "Request encoding: " . mb_detect_encoding($_POST["sync"]) . "\n";
	echo "Request text:" . $_POST["sync"];
	exit();
}

$database = new stdClass;
$syncTimeStart = $syncData->lastSync;
$explicitCheckOfRows = array();
$explicitCheckOfDeletes = "";






// Assign real deviceID if it was not assigned 
if ($syncData->deviceIdStatus == 0) {
	
	$newId = 1; 
	
	sqlQuery("BEGIN");
	$query = "SELECT MAX(deviceid) FROM __SQLsyncDevices";
	$res = sqlQuery($query);
	if (sqlNumRows($res)) {
		$currentMaxId = sqlFetchRow($res);
		$newId = $currentMaxId[0];
	}
	
	$decode = octdec(str_replace("8", "0", $newId));
	$newId = str_replace("0", "8", decoct($decode + 1));
	 
	$query = "INSERT INTO __SQLsyncDevices(androidid, deviceid) VALUES ('". sqlEscapeString($syncData->androidId) ."',$newId)";
	$res = sqlQuery($query);	
	sqlQuery("COMMIT");
	
	$database->newDeviceId = $newId;
}	
else $database->newDeviceId = 0;





// This changes data in global DB and resolving conflicts that might happen
foreach ($syncData->tableNames as $tableName) {
	$structure = 	$syncData->{"struct_" . $tableName};
	$structureImp = implode(",", $structure);		
		
	

	
	//Updates changed data
	$tableData = $syncData->{"updData_" . $tableName};
	$tableGID  = $syncData->{"updGID_" . $tableName};
	$tableLC   = $syncData->{"updLC_"   . $tableName};
	$tableDeletes = array();	
	$explicitCheckOfRows[$tableName] = array();
	
	//UPDATE: resolves confict vs update
	foreach ($tableGID as $i => $gid) {

		// mobile UPDATE vs global UPDATE conflict
		$query = "SELECT * FROM $tableName WHERE ". SyncSettings::SYNC_ID_COL ."=". $gid ." LIMIT 1";
		$res = sqlQuery($query);
		$wins = 1; // 1 = global, 2 = mobile
		
		if (sqlNumRows($res)) {
			$row = sqlFetchArray($res);
			
			$values = array();
			$nCol = 0;
			foreach ($structure as $colName) {
				if (!is_null($tableData[$i][$nCol])) 
					$values[] = $colName ."='". sqlEscapeString($tableData[$i][$nCol]) . "'"; 
				else 
					$values[] = $colName ."=NULL"; 
				
				$nCol++;
			}
				
			$query = "UPDATE $tableName SET ". implode(',', $values) ." WHERE ". SyncSettings::SYNC_ID_COL ."=".$tableGID[$i] . " AND ".
				//Condition for resolution (most recent wins)
				"_lastChange<'". $tableLC[$i] . " UTC'";
				
			$rq = sqlQuery($query);
			if (sqlAffectedRows($rq) == 1)
			{
				$wins = 2;
				
				//Logs discarded values
				if ($row['_lastchange'] > $syncTimeStart) {

					$lastChange = $row['_lastchange'];
					unset($row[SyncSettings::SYNC_ID_COL]);
					unset($row['_lastchange']);

					$query = "INSERT INTO __SQLsyncLogs VALUES ('$tableName',$gid,'". $lastChange . "','".
						sqlEscapeString(json_encode($row)) ."')";				
					sqlQuery($query);
				}				
			}
			else {
				$wins = 1;

				// register changes that might have happened during sync
				if ($row['_lastchange'] <= $syncTimeStart) {
					$explicitCheckOfRows[$tableName][] = $gid;
				}
			}
		}
		// INSERTs rows if its not present in database and between deleted rows
		else {		
			sqlQuery("BEGIN");
			$query = "SELECT ". SyncSettings::SYNC_ID_COL ." FROM __SQLsyncDeletedRows WHERE ". SyncSettings::SYNC_ID_COL ."=". $gid ." AND tableName='$tableName' LIMIT 1";
			$res = sqlQuery($query);
		
			if (!sqlNumRows($res)) {
				$query = "INSERT INTO $tableName(". SyncSettings::SYNC_ID_COL ."," . $structureImp . ") VALUES ($gid,".
				implodeVal($tableData[$i]) . ")";				
				sqlQuery($query);
			}
			sqlQuery("COMMIT");
		}
		
		
		//Logs discarded values
		if ($wins == 1) {
			$logData = array();

			foreach ($structure as $nCol => $colName) {
				$logData[$colName] = $tableData[$i][$nCol];
			}

			$query = "INSERT INTO __SQLsyncLogs VALUES ('$tableName',$gid,'". $tableLC[$i]  ." UTC','". 
				sqlEscapeString(json_encode($logData)) ."')";				
			sqlQuery($query);
		}
		 
	}
	
}


// mobile DELETE vs global UPDATE conflict
$tableData = $syncData->deletedRows;

foreach ($tableData as $deletedRow) {	
	$query = "SELECT * FROM ". $deletedRow[0] ." WHERE ". SyncSettings::SYNC_ID_COL ."=". $deletedRow[1] ." LIMIT 1";
	$res = sqlQuery($query);
	if (sqlNumRows($res)) {
		$row = sqlFetchArray($res);

		$query = "DELETE FROM ". $deletedRow[0] ." WHERE ". SyncSettings::SYNC_ID_COL ."=". $deletedRow[1];
		$rs = sqlQuery($query);

		// register changes that might have happened during last sync and were not registered
		if (sqlAffectedRows($rs) == 0) {
			if ($row['_lastchange'] <= $syncTimeStart) {
				$explicitCheckOfRows[$deletedRow[0]][] = $deletedRow[1];
			}
		}
		else {
			
			$lastChange = $row['_lastchange'];
			unset($row[SyncSettings::SYNC_ID_COL]);
			unset($row['_lastchange']);

			//Logs deleted row
			$query = "INSERT INTO __SQLsyncLogs VALUES ('". $deletedRow[0] ."',". $deletedRow[1] .",'". 
				$lastChange ."','". sqlEscapeString(json_encode($row)) ."')";				
			sqlQuery($query);		
		}		
	}
}






// Extracts data and sends it back to mobile device
$query = "SELECT CURRENT_TIMESTAMP";
$res = sqlQuery($query);
$res = sqlFetchRow($res);
	
$syncTimeEnd = $res[0];
	
	
foreach ($syncData->tableNames as $tableName) {	

	$structure = 	$syncData->{"struct_" . $tableName};
	$structureImp = implode(",", $structure);
	
	
	// UPDATEs
	$query = "SELECT ". SyncSettings::SYNC_ID_COL .",$structureImp FROM $tableName WHERE ".
		"(_lastChange>'$syncTimeStart' AND _lastChange<='$syncTimeEnd')";

	if (!empty($explicitCheckOfRows[$tableName])) {
		$query .= " OR ". SyncSettings::SYNC_ID_COL ." IN (". implode(",", $explicitCheckOfRows[$tableName]) .")";
	}

	$res = sqlQuery($query);

	$table = array();
	while ($array = sqlFetchRow($res)) {
		$table[] = $array; 
	}
	
	$database->{"updData_" . $tableName} = $table;
	
	//Send table structure back		
	$struct_gid = SyncSettings::SYNC_ID_COL;
	array_unshift($structure, $struct_gid);	
	$database->{"struct_" . $tableName} = $structure;		
}





// DELETEs
$query = "SELECT ". SyncSettings::SYNC_ID_COL .",tableName FROM __SQLsyncDeletedRows WHERE ".
	"(_timeDeleted>'$syncTimeStart' AND _timeDeleted<='$syncTimeEnd')";
if ($explicitCheckOfDeletes != "") {
	$query .= $explicitCheckOfDeletes;
}
	
$res = sqlQuery($query);
$table = array();

while ($array = sqlFetchRow($res)) {
	$table[] = $array; 
}
$database->deletedRows = $table;	
	



	
$database->lastSync = $syncTimeEnd;

//Sends response back
echo json_encode($database);
sqlConnectClose();

?>