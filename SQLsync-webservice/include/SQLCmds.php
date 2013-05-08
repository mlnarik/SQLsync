<?
include 'SyncSettings.php';


function sqlConnect() {
	$connect = 
		  "host='".     SyncSettings::SQL_SERVER   .
		"' port='".     SyncSettings::SQL_PORT     .
		"' dbname='".   SyncSettings::SQL_DB_NAME  .
		"' user='".     SyncSettings::SQL_LOGIN    .
		"' password='". SyncSettings::SQL_PASSWORD .
		"' ";
		
	pg_connect($connect) or die("SQL connect failed!");
}

function sqlQuery($query) {
	$res = pg_query($query);
	if (!$res) {	
		echo "SqlError in query: " . $query . "\n";
		echo pg_last_error() . "\n";
	}
	return $res;
}

function sqlConnectClose() {
	pg_close();
}

function sqlFetchRow($source) {	
	return pg_fetch_row($source);
}

function sqlFetchArray($source) {	
	return pg_fetch_array($source,NULL,PGSQL_ASSOC);
}

function sqlNumRows($source)
{
	return pg_num_rows($source);
}

function sqlAffectedRows($source)
{
	return pg_affected_rows($source);
}

function sqlLastID() {
	$res = pg_query("SELECT lastval()");
	if ($id = pg_fetch_row($res)) {
		return $id[0];
	}	
	return false;
}

function sqlEscapeString($source) {
	return pg_escape_string($source);
}

/*
// MySQL functions
function sqlConnect() {
	mysql_connect("127.0.0.1","polycarbonate.cz","testpass11") or die("SQL connect failed!");
	mysql_select_db("polycarbonatecz1") or die("Wrong database!");
}

function sqlQuery($query) {
	$res = mysql_query($query);
	if (mysql_errno() != 0) {	
		echo "SqlError in query: " . $query . "\n";
		echo mysql_error() . "\n";
	}
	return $res;
}

function sqlConnectClose() {
	mysql_close();
}

function sqlFetchRow($source) {
	return mysql_fetch_row($source);
}

function sqlFetchArray($source) {
	return mysql_fetch_array($source, MYSQL_ASSOC);
}

function sqlNumRows($source)
{
	return mysql_num_rows($source);
}

function sqlAffectedRows($source)
{
	return mysql_affected_rows();
}

function sqlLastID() {
	return mysql_insert_id();
}

function sqlEscapeString($source) {
	return mysql_escape_string($source);
}
*/
?>