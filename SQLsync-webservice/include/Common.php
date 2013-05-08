<?

function implodeVal($array) {
	$tmp = array();
	foreach ($array as $i => &$value) {
		if (is_null($value)) $tmp[$i] = "NULL";
		else $tmp[$i] = "'". sqlEscapeString($value) ."'";
	}
	return implode(',',$tmp);
} 


?>