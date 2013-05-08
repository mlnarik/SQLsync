<?

header('Content-Type: text/html; charset=utf-8');

error_reporting(E_ALL);
ini_set('display_errors', 1);

include '../include/SQLCmds.php';

$tableName1 = "products";
$tableName2 = "customers";

sqlConnect();

$nRows = 50;

if (@$_GET["a"] == "insert") {

	sqlQuery("TRUNCATE TABLE $tableName1");
	sqlQuery("TRUNCATE TABLE $tableName2");
	sqlQuery("TRUNCATE TABLE __SQLsyncDeletedRows");
	
	for ($i=1; $i<=$nRows; $i++) {
		$query = "INSERT INTO {$tableName1}(name, count, price) VALUES 
			('kava $i', $i*10, $i*100)";
		$res = sqlQuery($query); 
	}
}
else if (@$_GET["a"] == "echo") {
	echo "START\n";
	
	$query = "SELECT * FROM ". $GLOBALS['tableName1']." ORDER BY _syncid";
	$res = sqlQuery($query);
	while ($row = sqlFetchArray($res)) {
		echo $row['_syncid'] .",". $row['name'] .",". $row['count'] .",". $row['price'] . "\n";
	}	
	
	echo "DONE\n\n";
}
else if (@$_GET["a"] == "update") {
	
	//$r = $_GET["r"];
	
	for ($i=0; $i<$nRows; $i++) {
		
		$query = "UPDATE {$tableName1} SET count=count+1, price=price+2 WHERE _syncid = 
			(SELECT _syncid FROM {$tableName1} ORDER BY _syncid LIMIT 1 OFFSET $i)";
		$res = sqlQuery($query);
	} 
}
else if (@$_GET["a"] == "generate_init") {

for ($i=0; $i<10; $i++) {
	$query = "INSERT INTO {$tableName}(nazev, pocet, cena) VALUES 
		('stul', random()*100, random()*10000)";
	$res = sqlQuery($query);	
}

}
else if (@$_GET["a"] == "generate") {

	$query = "UPDATE $tableName SET pocet=random()*100, cena=random()*10000 WHERE 
		_globalID=(SELECT _globalID FROM $tableName WHERE _globalID>3 ORDER BY random() LIMIT 1)";
	$res = sqlQuery($query);
	
	$query = "DELETE FROM $tableName WHERE 
		_globalID=(SELECT _globalID FROM $tableName WHERE _globalID>3 ORDER BY random() LIMIT 1)";
	$res = sqlQuery($query);	
	
	$query = "INSERT INTO {$tableName}(nazev, pocet, cena) VALUES 
		('stul', random()*100, random()*10000)";
	$res = sqlQuery($query);

}



?>