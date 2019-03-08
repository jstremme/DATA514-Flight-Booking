import java.io.FileInputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Runs queries against a back-end database
 */
public class Query
{
  private String configFilename;
  private Properties configProps = new Properties();

  private String jSQLDriver;
  private String jSQLUrl;
  private String jSQLUser;
  private String jSQLPassword;

  // DB Connection
  private Connection conn;

  // Logged In User
  private static String username; // customer username is unique

  // Logged In Statust
  private static int loggedin = 0; // customer username is unique

  // Canned queries

  private static final String CHECK_FLIGHT_CAPACITY = "SELECT capacity FROM Flights WHERE fid = ?";
  private PreparedStatement checkFlightCapacityStatement;

  // transactions
  private static final String BEGIN_TRANSACTION_SQL = "SET TRANSACTION ISOLATION LEVEL SERIALIZABLE; BEGIN TRANSACTION;";
  private PreparedStatement beginTransactionStatement;

  private static final String COMMIT_SQL = "COMMIT TRANSACTION";
  private PreparedStatement commitTransactionStatement;

  private static final String ROLLBACK_SQL = "ROLLBACK TRANSACTION";
  private PreparedStatement rollbackTransactionStatement;

  class Flight
  {
    public int fid;
    public int year;
    public int monthId;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public double time;
    public int capacity;
    public double price;

    @Override
    public String toString()
    {
      return "ID: " + fid + " Date: " + year + "-" + monthId + "-" + dayOfMonth + " Carrier: " + carrierId +
              " Number: " + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time +
              " Capacity: " + capacity + " Price: " + price;
    }
  }

  public Query(String configFilename)
  {
    this.configFilename = configFilename;
  }

  /* Connection code to SQL Azure.  */
  public void openConnection() throws Exception
  {
    configProps.load(new FileInputStream(configFilename));

    jSQLDriver = configProps.getProperty("flightservice.jdbc_driver");
    jSQLUrl = configProps.getProperty("flightservice.url");
    jSQLUser = configProps.getProperty("flightservice.sqlazure_username");
    jSQLPassword = configProps.getProperty("flightservice.sqlazure_password");

		/* load jdbc drivers */
    Class.forName(jSQLDriver).newInstance();

		/* open connections to the flights database */
    conn = DriverManager.getConnection(jSQLUrl, // database
            jSQLUser, // user
            jSQLPassword); // password

    conn.setAutoCommit(true); //by default automatically commit after each statement

		/* You will also want to appropriately set the transaction's isolation level through:
		   conn.setTransactionIsolation(...)
		   See Connection class' JavaDoc for details.
		 */
  }

  public void closeConnection() throws Exception
  {
    conn.close();
  }

  /**
   * Clear the data in any custom tables created. Do not drop any tables and do not
   * clear the flights table. You should clear any tables you use to store reservations
   * and reset the next reservation ID to be 1.
   */
  public void clearTables ()
  {
    // your code here
  }

	/**
   * prepare all the SQL statements in this method.
   * "preparing" a statement is almost like compiling it.
   * Note that the parameters (with ?) are still not filled in
   */
  public void prepareStatements() throws Exception
  {
    beginTransactionStatement = conn.prepareStatement(BEGIN_TRANSACTION_SQL);
    commitTransactionStatement = conn.prepareStatement(COMMIT_SQL);
    rollbackTransactionStatement = conn.prepareStatement(ROLLBACK_SQL);

    checkFlightCapacityStatement = conn.prepareStatement(CHECK_FLIGHT_CAPACITY);

    /* add here more prepare statements for all the other queries you need */
		/* . . . . . . */
  }

  /**
   * Takes a user's username and password and attempts to log the user in.
   *
   * @param username
   * @param password
   *
   * @return If someone has already logged in, then return "User already logged in\n"
   * For all other errors, return "Login failed\n".
   *
   * Otherwise, return "Logged in as [username]\n".
   */
  public String transaction_login(String username, String password)
  {

    try
      {
        beginTransaction();

        if (loggedin == 1)
        {
          return "User already logged in\n";
        }

        String selectSQL =
            "Select password FROM USERS "
              + "WHERE username = \'" + username + "\'";
        Statement selectStatement = conn.createStatement();
        ResultSet result = selectStatement.executeQuery(selectSQL);
        result.next();
        String p = result.getString("password");
        if (!(p.equals(password)))
        {
          return "Login failed\n";
        }
        // String updateSQL =
        // "Update USERS Set loggedin = 1 "
        //         + "WHERE username = \'" + username + "\'" + " AND password = \'" + password + "\'" + " AND loggedin = 0";
        // Statement updateStatement = conn.createStatement();
        // updateStatement.executeUpdate(updateSQL);
        Query.username = username;
        Query.loggedin = 1;

        String itDelSQL = "TRUNCATE TABLE ITINERARIES";
        Statement itDelStatement = conn.createStatement();
        itDelStatement.executeUpdate(itDelSQL);

        String iidDelSQL = "TRUNCATE TABLE IIDTRACK";
        Statement iidDelStatement = conn.createStatement();
        iidDelStatement.executeUpdate(iidDelSQL);

        commitTransaction();
        return "Logged in as " + username + "\n";
      } 
      catch (SQLException e)
      { 
        e.printStackTrace(); 
        // rollbackTransaction();
        return "Login failed\n";
      }   
  }

  /**
   * Implement the create user function.
   *
   * @param username new user's username. User names are unique the system.
   * @param password new user's password.
   * @param initAmount initial amount to deposit into the user's account, should be >= 0 (failure otherwise).
   *
   * @return either "Created user {@code username}\n" or "Failed to create user\n" if failed.
   */
  public String transaction_createCustomer (String username, String password, double initAmount)
  {   
      try
      {
        beginTransaction();
        String selectSQL = 
            "SELECT 1 FROM Users "
            + "WHERE username = \'" + username + "\'";
        Statement selectStatement = conn.createStatement();
        ResultSet r = selectStatement.executeQuery(selectSQL);

        if (r.next() == true || initAmount < 0)
        {
          rollbackTransaction();
          return "Failed to create user\n";
        }

        String insertSQL =
            "INSERT INTO USERS "
                + "VALUES(\'" + username + "\',\'" + password + "\'," + initAmount + ") ";
   
        Statement insertStatement = conn.createStatement();
        insertStatement.executeUpdate(insertSQL);
        commitTransaction();
        return "Created user " + username + "\n";
      } 
      catch (SQLException e) 
      { 
        // rollbackTransaction();
        e.printStackTrace(); 
        return "Failed to create user\n";
      }
  }

  /**
   * Implement the search function.
   *
   *
   * NOTE, function ignores year and month and considers "same day" to mean same day of the month
   *
   *
   * Searches for flights from the given origin city to the given destination
   * city, on the given day of the month. If {@code directFlight} is true, it only
   * searches for direct flights, otherwise is searches for direct flights
   * and flights with two "hops." Only searches for up to the number of
   * itineraries given by {@code numberOfItineraries}.
   *
   * The results are sorted based on total flight time.
   *
   * @param originCity
   * @param destinationCity
   * @param directFlight if true, then only search for direct flights, otherwise include indirect flights as well
   * @param dayOfMonth
   * @param numberOfItineraries number of itineraries to return
   *
   * @return If no itineraries were found, return "No flights match your selection\n".
   * If an error occurs, then return "Failed to search\n".
   *
   * Otherwise, the sorted itineraries printed in the following format:
   *
   * Itinerary [itinerary number]: [number of flights] flight(s), [total flight time] minutes\n
   * [first flight in itinerary]\n
   * ...
   * [last flight in itinerary]\n
   *
   * Each flight should be printed using the same format as in the {@code Flight} class. Itinerary numbers
   * in each search should always start from 0 and increase by 1.
   *
   * @see Flight#toString()
   */
  public String transaction_search(String originCity, String destinationCity, boolean directFlight, int dayOfMonth,
                                   int numberOfItineraries)
  {
    // return transaction_search_unsafe(originCity, destinationCity, directFlight, dayOfMonth, numberOfItineraries);
    return transaction_search_safe(originCity, destinationCity, directFlight, dayOfMonth, numberOfItineraries);
  }

  /**
   * Same as {@code transaction_search} except that it only performs single hop search and
   * do it in an unsafe manner.
   *
   * @param originCity
   * @param destinationCity
   * @param directFlight
   * @param dayOfMonth
   * @param numberOfItineraries
   *
   * @return The search results. Note that this implementation *does not conform* to the format required by
   * {@code transaction_search}.
   */
  private String transaction_search_safe(String originCity, String destinationCity, boolean directFlight,
                                          int dayOfMonth, int numberOfItineraries)
  {    
    try 
    {
      beginTransaction();

      String itDelSQL = "TRUNCATE TABLE ITINERARIES";
      Statement itDelStatement = conn.createStatement();
      itDelStatement.executeUpdate(itDelSQL);

      String iidDelSQL = "TRUNCATE TABLE IIDTRACK";
      Statement iidDelStatement = conn.createStatement();
      iidDelStatement.executeUpdate(iidDelSQL);

      int numAdded = 0;
      if (directFlight) 
      {
        int n = add_direct_flights(originCity, destinationCity, directFlight, dayOfMonth, numberOfItineraries);
        numAdded = numAdded + n;
      }
      else 
      {
        int n = add_direct_flights(originCity, destinationCity, directFlight, dayOfMonth, numberOfItineraries);
        numAdded = numAdded + n;
        int numIndirects = numberOfItineraries - numAdded;
        int m = add_indirect_flights(originCity, destinationCity, directFlight, dayOfMonth, numIndirects);
        numAdded = numAdded + m;
      }
      if (numAdded == 0)
      {
        return "No flights match your selection\n";
      }
      commitTransaction();
      return "\n";
    }
    catch (SQLException e)
    {
      e.printStackTrace(); 
      return "Failed to search\n";
    }
  }

  private int add_direct_flights(String originCity, String destinationCity, boolean directFlight,
                                    int dayOfMonth, int numberOfItineraries) throws SQLException
  {
    String directSearchSQL =
      "SELECT TOP (" + numberOfItineraries + ") fid, year, day_of_month, carrier_id, flight_num, origin_city, dest_city, actual_time, capacity, price "
      + "FROM Flights "
      + "WHERE origin_city = \'" + originCity + "\' "
      + "AND dest_city = \'" + destinationCity + "\' "
      + "AND day_of_month =  " + dayOfMonth + " "
      + "AND canceled = 0 "
      + "ORDER BY actual_time ASC, fid ASC";

    PreparedStatement dirPreparedStatement = conn.prepareStatement(directSearchSQL);
    ResultSet oneHopResults = dirPreparedStatement.executeQuery();

    String selectSQL = 
        "SELECT TOP(1) iid FROM IIDTRACK "
            + "ORDER BY iid DESC";
    Statement selectStatement = conn.createStatement();
    ResultSet r = selectStatement.executeQuery(selectSQL);
    int iid = 0;
    if (r.next())
    {
      iid = r.getInt("iid") + 1;
    }

    int numFlights = 0;
    while (oneHopResults.next())
    {
      int fid = oneHopResults.getInt("fid");
      int year = oneHopResults.getInt("year");
      int day_of_month = oneHopResults.getInt("day_of_month");
      String carrier_id = oneHopResults.getString("carrier_id");
      int flight_num = oneHopResults.getInt("flight_num");
      String origin_city = oneHopResults.getString("origin_city");
      String dest_city = oneHopResults.getString("dest_city");
      float actual_time = oneHopResults.getFloat("actual_time");
      int capacity = oneHopResults.getInt("capacity");
      float price = oneHopResults.getFloat("price");

      System.out.println("Itinerary " + iid + ": " + 1 + " flight(s), " + actual_time + " minutes");
      System.out.println("ID: " + fid + " Date: " + year + "-7-" + day_of_month + " Carrier: " + carrier_id + " Number: " + flight_num + " Origin: " + origin_city + " Dest: " + dest_city + " Duration: " + actual_time + " Capacity: " + capacity + " Price: " + price);

      String itInsertSQL =
          "INSERT INTO ITINERARIES "
            + "VALUES(" + iid + "," + fid + "," + 0 + "," + price + ")";
      Statement itInsertStatement = conn.createStatement();
      itInsertStatement.executeUpdate(itInsertSQL);

      String iidInsertSQL =
          "INSERT INTO IIDTRACK "
            + "VALUES(" + iid + ")";
      Statement iidInsertStatement = conn.createStatement();
      iidInsertStatement.executeUpdate(iidInsertSQL);
      iid++;
      numFlights++;
    }
    oneHopResults.close();
    return numFlights;
  }

  private int add_indirect_flights(String originCity, String destinationCity, boolean directFlight,
                                      int dayOfMonth, int numberOfItineraries) throws SQLException
  {
    String indirectSearchSQL =
      "SELECT DISTINCT TOP (" + numberOfItineraries + ") f1.fid as fid_a, f1.year as year_a, f1.day_of_month as day_of_month_a, f1.carrier_id as carrier_id_a, f1.flight_num as flight_num_a, f1.origin_city as origin_city_a, f1.dest_city as dest_city_a, f1.actual_time as actual_time_a, f1.capacity as capacity_a, f1.price as price_a, f2.fid as fid_b, f2.year as year_b, f2.day_of_month as day_of_month_b, f2.carrier_id as carrier_id_b, f2.flight_num as flight_num_b, f2.origin_city as origin_city_b, f2.dest_city as dest_city_b, f2.actual_time as actual_time_b, f2.capacity as capacity_b, f2.price as price_b, (f1.actual_time + f2.actual_time) as total_time "
      + "FROM Flights as f1, Flights as f2 "
      + "WHERE f1.origin_city = \'" + originCity + "\' "
      + "AND f1.dest_city = f2.origin_city "
      + "AND f2.dest_city = \'" + destinationCity + "\' "
      + "AND f1.day_of_month =  " + dayOfMonth + " "
      + "AND f2.day_of_month =  " + dayOfMonth + " "
      + "AND f1.actual_time IS NOT NULL "
      + "AND f2.actual_time IS NOT NULL "
      + "AND f1.canceled = 0 "
      + "AND f2.canceled = 0 "
      + "ORDER BY total_time ASC, fid_a ASC, fid_b ASC";

    PreparedStatement inPreparedStatement = conn.prepareStatement(indirectSearchSQL);
    ResultSet indirectResults = inPreparedStatement.executeQuery();

    String selectSQL = 
        "SELECT TOP(1) iid FROM IIDTRACK "
            + "ORDER BY iid DESC";
    Statement selectStatement = conn.createStatement();
    ResultSet r = selectStatement.executeQuery(selectSQL);
    int iid = 0;
    if (r.next())
    {
      iid = r.getInt("iid") + 1;
    }

    int numFlights = 0;
    while (indirectResults.next())
    {
      int fid_a = indirectResults.getInt("fid_a");
      int year_a = indirectResults.getInt("year_a");
      int day_of_month_a = indirectResults.getInt("day_of_month_a");
      String carrier_id_a = indirectResults.getString("carrier_id_a");
      int flight_num_a = indirectResults.getInt("flight_num_a");
      String origin_city_a = indirectResults.getString("origin_city_a");
      String dest_city_a = indirectResults.getString("dest_city_a");
      float actual_time_a = indirectResults.getFloat("actual_time_a");
      int capacity_a = indirectResults.getInt("capacity_a");
      float price_a = indirectResults.getFloat("price_a");

      int fid_b = indirectResults.getInt("fid_b");
      int year_b = indirectResults.getInt("year_b");
      int day_of_month_b = indirectResults.getInt("day_of_month_b");
      String carrier_id_b = indirectResults.getString("carrier_id_b");
      int flight_num_b = indirectResults.getInt("flight_num_b");
      String origin_city_b = indirectResults.getString("origin_city_b");
      String dest_city_b = indirectResults.getString("dest_city_b");
      float actual_time_b = indirectResults.getFloat("actual_time_b");
      int capacity_b = indirectResults.getInt("capacity_b");
      float price_b = indirectResults.getFloat("price_b");

      float total_time = indirectResults.getFloat("total_time");
      float total_price = price_a + price_b;

      System.out.println("Itinerary " + iid + ": " + 2 + " flight(s), " + total_time + " minutes");
      System.out.println("ID: " + fid_a + " Date: " + year_a + "-7-" + day_of_month_a + " Carrier: " + carrier_id_a + " Number: " + flight_num_a + " Origin: " + origin_city_a + " Dest: " + dest_city_a + " Duration: " + actual_time_a + " Capacity: " + capacity_a + " Price: " + price_a);
      System.out.println("ID: " + fid_b + " Date: " + year_b + "-7-" + day_of_month_b + " Carrier: " + carrier_id_b + " Number: " + flight_num_b + " Origin: " + origin_city_b + " Dest: " + dest_city_b + " Duration: " + actual_time_b + " Capacity: " + capacity_b + " Price: " + price_b);

      String itInsertSQL =
          "INSERT INTO ITINERARIES "
            + "VALUES(" + iid + "," + fid_a + "," + fid_b + "," + total_price + ")";
      Statement itInsertStatement = conn.createStatement();
      itInsertStatement.executeUpdate(itInsertSQL);

      String iidInsertSQL =
          "INSERT INTO IIDTRACK "
            + "VALUES(" + iid + ")";
      Statement iidInsertStatement = conn.createStatement();
      iidInsertStatement.executeUpdate(iidInsertSQL);
      iid++;
      numFlights++;
    }
    indirectResults.close();
    return numFlights;
  }


  /**
   * Implements the book itinerary function.
   *
   * @param itineraryId ID of the itinerary to book. This must be one that is returned by search in the current session.
   *
   * @return If the user is not logged in, then return "Cannot book reservations, not logged in\n".
   * If try to book an itinerary with invalid ID, then return "No such itinerary {@code itineraryId}\n".
   * If the user already has a reservation on the same day as the one that they are trying to book now, then return
   * "You cannot book two flights in the same day\n".
   * For all other errors, return "Booking failed\n".
   *
   * And if booking succeeded, return "Booked flight(s), reservation ID: [reservationId]\n" where
   * reservationId is a unique number in the reservation system that starts from 1 and increments by 1 each time a
   * successful reservation is made by any user in the system.
   */
  public String transaction_book(int itineraryId)
  {
    try
      {
        beginTransaction();

        // String logSelectSQL = 
        //     "SELECT loggedin FROM USERS "
        //     + "WHERE username = \'" + username + "\'";
        // Statement logSelectStatement = conn.createStatement();
        // ResultSet logResult = logSelectStatement.executeQuery(logSelectSQL);
        // int loggedIn = logResult.getInt("loggedin");
        if (loggedin == 0)
        {
          return "Cannot book reservations, not logged in\n";
        }

        String iidSelectSQL = 
            "SELECT iid FROM ITINERARIES ";
        Statement iidSelectStatement = conn.createStatement();
        ResultSet iidResult = iidSelectStatement.executeQuery(iidSelectSQL);
        ArrayList<Integer> iids = new ArrayList<>();
        while (iidResult.next())
        {
          int iid = iidResult.getInt("iid");
          iids.add(iid);
        }
        if (!(iids.contains(itineraryId)))
        {
          return "No such itinerary " + itineraryId + "\n";
        }

        String rSelectSQL = 
            "SELECT 1 FROM ITINERARIES as i, RESERVATIONS as r, FLIGHTS as f "
            + "WHERE i.iid = " + itineraryId + " "
            + "AND i.fid_a = f.fid "
            + "AND f.year = r.year "
            + "AND f.day_of_month = r.day_of_month "
            + "AND r.username = \'" + username + "\'";

        PreparedStatement rPreparedStatement = conn.prepareStatement(rSelectSQL);
        ResultSet rResult = rPreparedStatement.executeQuery();

        if (rResult.next()) {
          return "You cannot book two flights in the same day\n";
        }
        
        String selectSQL = 
        "SELECT TOP(1) rid FROM RIDTRACK "
            + "ORDER BY rid DESC";
        Statement selectStatement = conn.createStatement();
        ResultSet r = selectStatement.executeQuery(selectSQL);
        int rid = 0;
        if (r.next())
        {
          rid = r.getInt("rid") + 1;
        }

        String iSelectSQL = 
        "SELECT i.fid_a as fid_a, i.fid_b as fid_b, f.year as year, f.day_of_month as day_of_month, i.cost as cost FROM ITINERARIES as i, FLIGHTS as f "
            + "WHERE i.iid = " + itineraryId + " "
            + "AND i.fid_a = f.fid ";

        PreparedStatement iPreparedStatement = conn.prepareStatement(iSelectSQL);
        ResultSet i = iPreparedStatement.executeQuery();
        i.next();
        int fid_a = i.getInt("fid_a");
        int fid_b = i.getInt("fid_b");
        int year = i.getInt("year");
        int day_of_month = i.getInt("day_of_month");
        float cost = i.getFloat("cost");
        int paid = 0;

        String rInsertSQL =
            "INSERT INTO RESERVATIONS "
                + "VALUES(" + rid + "," + "\'" + username + "\'," + fid_a + "," + fid_b + "," + year + "," + day_of_month + "," + cost + "," + paid + ")";
        Statement rInsertStatement = conn.createStatement();
        rInsertStatement.executeUpdate(rInsertSQL);

        String ridInsertSQL =
            "INSERT INTO RIDTRACK "
                + "VALUES(" + rid + ")";
        Statement ridInsertStatement = conn.createStatement();
        ridInsertStatement.executeUpdate(ridInsertSQL);

        commitTransaction();
        return "Booked flight(s), reservation ID: " + rid + "\n";

      } 
      catch (SQLException e) 
      { 
        // rollbackTransaction();
        e.printStackTrace(); 
        return "Booking failed\n";
      }
  }

  /**
   * Implements the reservations function.
   *
   * @return If no user has logged in, then return "Cannot view reservations, not logged in\n"
   * If the user has no reservations, then return "No reservations found\n"
   * For all other errors, return "Failed to retrieve reservations\n"
   *
   * Otherwise return the reservations in the following format:
   *
   * Reservation [reservation ID] paid: [true or false]:\n"
   * [flight 1 under the reservation]
   * [flight 2 under the reservation]
   * Reservation [reservation ID] paid: [true or false]:\n"
   * [flight 1 under the reservation]
   * [flight 2 under the reservation]
   * ...
   *
   * Each flight should be printed using the same format as in the {@code Flight} class.
   *
   * @see Flight#toString()
   */
  public String transaction_reservations()
  {
    try
      {
        beginTransaction();
        if (loggedin == 0)
        {
          return "Cannot view reservations, not logged in\n";
        }
        String rSelectSQL = 
            "SELECT 1 FROM RESERVATIONS "
            + "WHERE username = \'" + username + "\'";
        PreparedStatement rPreparedStatement = conn.prepareStatement(rSelectSQL);
        ResultSet rResult = rPreparedStatement.executeQuery();
        if (!(rResult.next()))
        {
          return "No reservations found\n";
        }

        String resSelectSQL = 
            "SELECT rid, fid_b, paid FROM RESERVATIONS "
            + "WHERE username = \'" + username + "\'";
        PreparedStatement resPreparedStatement = conn.prepareStatement(resSelectSQL);
        ResultSet resResult = resPreparedStatement.executeQuery();
        ArrayList<Integer> rids = new ArrayList<Integer>();
        ArrayList<Integer> fid_bs = new ArrayList<Integer>();
        ArrayList<Integer> paids = new ArrayList<Integer>();
        while (resResult.next())
        {
          int rid = resResult.getInt("rid");
          int fid_bc = resResult.getInt("fid_b");
          int paid = resResult.getInt("paid");
          rids.add(rid);
          fid_bs.add(fid_bc);
          paids.add(paid);
        }

        for(int i =0; i<rids.size(); i++)
        {
          int rid = rids.get(i);
          int fid_b_check = fid_bs.get(i);
          int paid = paids.get(i);
          int numFlights = 0;
          if (fid_b_check == 0) {
            numFlights = 1;
          }
          else {
            numFlights = 2;
          }

          String raSelectSQL = 
          "SELECT r.fid_a as fid_a, f.year as year_a, f.day_of_month as day_of_month_a, f.carrier_id as carrier_id_a, f.flight_num as flight_num_a, f.origin_city as origin_city_a, f.dest_city as dest_city_a, f.actual_time as actual_time_a, f.capacity as capacity_a, f.price as price_a FROM FLIGHTS as f, RESERVATIONS as r "
              + "WHERE r.username = \'" + username + "\'"
              + "AND r.rid = " + rid + " "
              + "AND r.fid_a = f.fid";
          PreparedStatement raPreparedStatement = conn.prepareStatement(raSelectSQL);
          ResultSet ra = raPreparedStatement.executeQuery();
          ra.next();

          int fid_a = ra.getInt("fid_a");
          int year_a = ra.getInt("year_a");
          int day_of_month_a = ra.getInt("day_of_month_a");
          String carrier_id_a = ra.getString("carrier_id_a");
          int flight_num_a = ra.getInt("flight_num_a");
          String origin_city_a = ra.getString("origin_city_a");
          String dest_city_a = ra.getString("dest_city_a");
          float actual_time_a = ra.getFloat("actual_time_a");
          int capacity_a = ra.getInt("capacity_a");
          float price_a = ra.getFloat("price_a");

          System.out.println("Reservation " + rid + " paid: " + paid);
          System.out.println("ID: " + fid_a + " Date: " + year_a + "-7-" + day_of_month_a + " Carrier: " + carrier_id_a + " Number: " + flight_num_a + " Origin: " + origin_city_a + " Dest: " + dest_city_a + " Duration: " + actual_time_a + " Capacity: " + capacity_a + " Price: " + price_a);

          if (numFlights == 2) 
          {
            String rbSelectSQL = 
            "SELECT r.fid_b as fid_b, f.year as year_b, f.day_of_month as day_of_month_b, f.carrier_id as carrier_id_b, f.flight_num as flight_num_b, f.origin_city as origin_city_b, f.dest_city as dest_city_b, f.actual_time as actual_time_b, f.capacity as capacity_b, f.price as price_b FROM FLIGHTS as f, RESERVATIONS as r "
                + "WHERE r.username = \'" + username + "\'"
                + "AND r.rid = " + rid + " "
                + "AND r.fid_b = f.fid";
            PreparedStatement rbPreparedStatement = conn.prepareStatement(rbSelectSQL);
            ResultSet rb = rbPreparedStatement.executeQuery();
            rb.next();

            int fid_b = rb.getInt("fid_b");
            int year_b = rb.getInt("year_b");
            int day_of_month_b = rb.getInt("day_of_month_b");
            String carrier_id_b = rb.getString("carrier_id_b");
            int flight_num_b = rb.getInt("flight_num_b");
            String origin_city_b =rb.getString("origin_city_b");
            String dest_city_b = rb.getString("dest_city_b");
            float actual_time_b = rb.getFloat("actual_time_b");
            int capacity_b = rb.getInt("capacity_b");
            float price_b = rb.getFloat("price_b");
          
            System.out.println("ID: " + fid_b + " Date: " + year_b + "-7-" + day_of_month_b + " Carrier: " + carrier_id_b + " Number: " + flight_num_b + " Origin: " + origin_city_b + " Dest: " + dest_city_b + " Duration: " + actual_time_b + " Capacity: " + capacity_b + " Price: " + price_b);
          }
        }
        commitTransaction();
      } 
      catch (SQLException e) 
      { 
        e.printStackTrace();
        return "Failed to retrieve reservations\n";
      }
    return "";
  }

  /**
   * Implements the cancel operation.
   *
   * @param reservationId the reservation ID to cancel
   *
   * @return If no user has logged in, then return "Cannot cancel reservations, not logged in\n"
   * For all other errors, return "Failed to cancel reservation [reservationId]"
   *
   * If successful, return "Canceled reservation [reservationId]"
   *
   * Even though a reservation has been canceled, its ID should not be reused by the system.
   */
  public String transaction_cancel(int reservationId)
  { 
    try 
    {
        beginTransaction();
        if (loggedin == 0)
        {
          return "Cannot cancel reservations, not logged in\n";
        }
        String delSQL = "DELETE FROM RESERVATIONS "
                  + "WHERE rid = " + reservationId + " ";
        Statement delStatement = conn.createStatement();
        delStatement.executeUpdate(delSQL);
        commitTransaction();
        return "Canceled reservation " + reservationId;
    }
    catch (SQLException e) 
    { 
      return "Failed to cancel reservation " + reservationId;
    }
  }

  /**
   * Implements the pay function.
   *
   * @param reservationId the reservation to pay for.
   *
   * @return If no user has logged in, then return "Cannot pay, not logged in\n"
   * If the reservation is not found / not under the logged in user's name, then return
   * "Cannot find unpaid reservation [reservationId] under user: [username]\n"
   * If the user does not have enough money in their account, then return
   * "User has only [balance] in account but itinerary costs [cost]\n"
   * For all other errors, return "Failed to pay for reservation [reservationId]\n"
   *
   * If successful, return "Paid reservation: [reservationId] remaining balance: [balance]\n"
   * where [balance] is the remaining balance in the user's account.
   */
  public String transaction_pay (int reservationId)
  {
    try
      {
        beginTransaction();
        // String logSelectSQL = 
        //     "SELECT loggedin FROM USERS "
        //     + "WHERE username = \'" + username + "\'";
        // Statement logSelectStatement = conn.createStatement();
        // ResultSet logResult = logSelectStatement.executeQuery(logSelectSQL);
        if (loggedin == 0)
        {
          return "Cannot pay, not logged in\n";
        }
        String rSelectSQL = 
            "SELECT rid FROM RESERVATIONS "
            + "WHERE rid = " + reservationId + " ";
        PreparedStatement rPreparedStatement = conn.prepareStatement(rSelectSQL);
        ResultSet rResult = rPreparedStatement.executeQuery();
        if (!(rResult.next()))
        {
          return "Cannot find unpaid reservation " + reservationId + " under user: \'" + username + "\'\n";
        }

        String uSelectSQL = 
            "SELECT balance FROM USERS "
            + "WHERE username = \'" + username + "\'";
        PreparedStatement uPreparedStatement = conn.prepareStatement(uSelectSQL);
        ResultSet uResult = uPreparedStatement.executeQuery();
        uResult.next();
        float balance = uResult.getFloat("balance");

        String resSelectSQL = 
            "SELECT cost FROM RESERVATIONS "
            + "WHERE rid = " + reservationId + " ";
        PreparedStatement resPreparedStatement = conn.prepareStatement(resSelectSQL);
        ResultSet resResult = resPreparedStatement.executeQuery();
        resResult.next();
        float cost = resResult.getFloat("cost");

        float remaining = balance - cost;
        if (remaining < 0)
        {
          return "User has only " + balance + " in account but itinerary costs " + cost + "\n";
        }
        else {
          String resUpdateSQL =
          "Update RESERVATIONS Set paid = 1 "
                  + "WHERE rid = " + reservationId + " ";
          Statement resUpdateStatement = conn.createStatement();
          resUpdateStatement.executeUpdate(resUpdateSQL);

          String userUpdateSQL =
          "Update USERS Set balance = " + remaining + " "
                  + "WHERE username = \'" + username + "\'";
          Statement userUpdateStatement = conn.createStatement();
          userUpdateStatement.executeUpdate(userUpdateSQL);

          String itDelSQL = "TRUNCATE TABLE ITINERARIES";
          Statement itDelStatement = conn.createStatement();
          itDelStatement.executeUpdate(itDelSQL);

          String iidDelSQL = "TRUNCATE TABLE IIDTRACK";
          Statement iidDelStatement = conn.createStatement();
          iidDelStatement.executeUpdate(iidDelSQL);

          commitTransaction();
          return "Paid reservation: " + reservationId + " remaining balance: " + remaining + "\n";
        }
      }
      catch (SQLException e) 
      { 
        e.printStackTrace(); 
        return "Failed to pay for reservation " + reservationId + "\n";
      }
  }

  /* some utility functions below */

  public void beginTransaction() throws SQLException
  {
    conn.setAutoCommit(false);
    beginTransactionStatement.executeUpdate();
  }

  public void commitTransaction() throws SQLException
  {
    commitTransactionStatement.executeUpdate();
    conn.setAutoCommit(true);
  }

  public void rollbackTransaction() throws SQLException
  {
    rollbackTransactionStatement.executeUpdate();
    conn.setAutoCommit(true);
  }

  /**
   * Shows an example of using PreparedStatements after setting arguments. You don't need to
   * use this method if you don't want to.
   */
  private int checkFlightCapacity(int fid) throws SQLException
  {
    checkFlightCapacityStatement.clearParameters();
    checkFlightCapacityStatement.setInt(1, fid);
    ResultSet results = checkFlightCapacityStatement.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }
}















