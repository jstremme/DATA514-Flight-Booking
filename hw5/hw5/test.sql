SELECT DISTINCT TOP (40) f1.fid as fid_a, f1.year as year_a, f1.day_of_month as day_of_month_a, f1.carrier_id as carrier_id_a, f1.flight_num as flight_num_a, f1.origin_city as origin_city_a, f1.dest_city as dest_city_a, f1.actual_time as actual_time_a, f1.capacity as capacity_a, f1.price as price_a, f2.fid as fid_b, f2.year as year_b, f2.day_of_month as day_of_month_b, f2.carrier_id as carrier_id_b, f2.flight_num as flight_num_b, f2.origin_city as origin_city_b, f2.dest_city as dest_city_b, f2.actual_time as actual_time_b, f2.capacity as capacity_b, f2.price as price_b, f1.actual_time + f2.actual_time as total_time
      FROM Flights as f1, Flights as f2
      WHERE f1.origin_city = 'Seattle WA'
      AND f1.dest_city = f2.origin_city
      AND f2.dest_city = 'Minneapolis MN'
      AND f1.day_of_month = 15
      AND f2.day_of_month = 15
      AND f1.actual_time IS NOT NULL
      AND f2.actual_time IS NOT NULL
      ORDER BY total_time, fid_a ASC, fid_b ASC;
