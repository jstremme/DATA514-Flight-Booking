-- add all your SQL setup statements here. 

-- You can assume that the following base table has been created with data loaded for you when we test your submission 
-- (you still need to create and populate it in your instance however),
-- although you are free to insert extra ALTER COLUMN ... statements to change the column 
-- names / types if you like.

-- CREATE TABLE FLIGHTS
-- (
--  fid int NOT NULL PRIMARY KEY,
--  year int,
--  month_id int,
--  day_of_month int,
--  day_of_week_id int,
--  carrier_id varchar(3),
--  flight_num int,
--  origin_city varchar(34),
--  origin_state varchar(47),
--  dest_city varchar(34),
--  dest_state varchar(46),
--  departure_delay double precision,
--  taxi_out double precision,
--  arrival_delay double precision,
--  canceled int,
--  actual_time double precision,
--  distance double precision,
--  capacity int,
--  price double precision
--)

CREATE TABLE USERS (
    username varchar(30),
    password varchar(30), 
    balance double precision
    PRIMARY KEY (username)
);

CREATE TABLE ITINERARIES (
    iid int,
    fid_a int,
    fid_b int,
    cost double precision,
    PRIMARY KEY (iid),
    FOREIGN KEY (fid_a) REFERENCES FLIGHTS(fid),
    FOREIGN KEY (fid_b) REFERENCES FLIGHTS(fid),
);

CREATE TABLE RESERVATIONS (
    rid int,
    username varchar(30),
    fid_a int,
    fid_b int,
    year int,
    day_of_month int,
    cost double precision,
    paid int,
    PRIMARY KEY (rid),
    FOREIGN KEY (username) REFERENCES USERS(username),
    FOREIGN KEY (fid_a) REFERENCES FLIGHTS(fid),
    FOREIGN KEY (fid_b) REFERENCES FLIGHTS(fid)
);

CREATE TABLE IIDTRACK (
    iid int
);

CREATE TABLE RIDTRACK (
    rid int
);

SELECT * FROM IIDTRACK;
SELECT * FROM RIDTRACK;
       
SELECT * FROM ITINERARIES;
SELECT * FROM USERS;
SELECT * FROM RESERVATIONS;