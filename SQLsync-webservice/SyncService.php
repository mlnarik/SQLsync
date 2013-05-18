<?

ini_set('display_errors', 1);
error_reporting(E_ALL);

//Enables GZ compression for output
@ob_start("ob_gzhandler");

include 'include/SQLCmds.php';

$authorized = false;

/**
 * Provide your custom authorization of mobile users.
 * This authorization is very basic and it might not be enough for your project.
 */
if ($_POST["name"] == SyncSettings::SYNC_USERNAME && $_POST["pass"] == SyncSettings::SYNC_PASSWORD)
	$authorized = true;
	

if($authorized) {

	/**
	 * Synchronization process
	 */
	if ($_GET["a"] == "sync") {
		include 'include/Sync.php';
	}
	else {		
		//echo "Wrong page";
		header("HTTP/1.0 404 Not Found");
	}
	// Add more services...
}
else {
	header("HTTP/1.0 403 Forbidden");
}


?>