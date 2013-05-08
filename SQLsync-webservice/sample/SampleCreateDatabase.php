<?

include '../include/InitDB.php';


sqlConnect();

sqlQuery("DROP TABLE IF EXISTS products");
sqlQuery("DROP TABLE IF EXISTS customers");


addSyncManagement();



$query = "CREATE TABLE products (
  _syncID int primary key not null default 0,
  name varchar(100) NOT NULL,
  count int NOT NULL,
  price int NOT NULL,
  alt_name varchar(100) DEFAULT NULL
);"; 
sqlQuery($query);	

addSyncTableColumns('products');

$query = "CREATE TABLE customers (
  _syncID int primary key not null default 0,
  name varchar(100) NOT NULL,
  address varchar(100) NOT NULL
);"; 
sqlQuery($query);	

addSyncTableColumns('customers');

sqlQuery("INSERT INTO products(name,count,price) VALUES ('nůžky',10,500),('stůl',2,4050),('židle',10,1400),
('tužka',111,15654),('repro',1120,112400),('maeko',1345,1564687)");
 
sqlConnectClose();

?>