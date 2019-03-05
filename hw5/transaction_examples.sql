-- transaction concurrency 
-- complicated
-- lots of overhead

-- key to this transaction: use 3 seat status'

Update SeatAssignments Set seatStatus = 'Hold', Customerid = 1
WHERE Flightid = 1 and seat = 4 and seatStatus = 'Free'

Update SeatAssignments Set seatStatus = 'Reserved'
WHERE Flightid = 1 and Customerid = 1 and seat = 4 and seatStatus = 'Hold'

-- writers acquire exclusive locks
	-- insert, delete, update keywords in SQL do this
-- readers acquire shared locks
	-- lock DB while doing some calculations
	-- i.e., what's the sum of sales right now?
-- use exclusive locks sparingly

-- "surge pricing" - a wonderful feature of capitalism
 

-- transaction for adding username
-- only goes if username doesn't already exist
insert name, pass, initamt
	where username != name

-- Read only DBs don't have to worry about locking
-- Except for the program that refreshes the extract from Prod
-- Ideal case for Business Intelligence program that do lots of reads


Begin transaction Hold;

Update SeatAssignments Set seatStatus = 'Hold', Customerid = 1
WHERE Flightid = 1 and seat = 4 and seatStatus = 'Free'

Update SeatAssignments Set seatStatus = 'Reserved'
WHERE Flightid = 1 and Customerid = 1 and seat = 4 and seatStatus = 'Hold'

Commit transaction Hold;

-- Or Rollback transaction Hold;
-- Use a try catch


-- can set transaction isolation level...

-- create user

