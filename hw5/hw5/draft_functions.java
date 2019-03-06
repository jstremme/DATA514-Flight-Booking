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
        String insertSQL =
            "INSERT INTO USERS "
                    + "VALUES(\'" + username + "\',\'" + password + "\'," + initAmount + ")"
                    + "WHERE username != \'" + username "\' AND " + initAmount + " >= 0";
        Statement insertStatement = conn.createStatement();
        insertStatement.executeUpdate(insertSQL);
        return "Created user " + username + "\n";
      } 
      catch (SQLException e) 
      { 
        // e.printStackTrace(); 
        return "Failed to create user\n";
      }
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
        String selectSQL =
            "Select loggedin FROM USERS"
                + "WHERE username = \'" + username "\' AND password = + \'" + password"\'";
        Statement selectStatement = conn.createStatement();
        ResultSet result = selectStatement.executeQuery(selectSQL);
        result.next();
        loggedin = result.getInt("loggedin");
        if (loggedin == 0) 
        {
          String updateSQL =
          "Update USERS Set loggedin = 1 "
                  + "WHERE username = \'" + username "\' AND password = + \'" + password"\'" + " AND loggedin = 0";
          Statement updateStatement = conn.createStatement();
          updateStatement.executeUpdate(updateSQL);
          commitTransaction();
          return "Logged in as " + username + "\n";
        } 
        else if (loggedin == 1) 
        {
          rollbackTransaction();
          return "User already logged in\n";
        }
        else 
        {
          rollbackTransaction();
          return "Login failed\n";
        }
      } 
      catch (SQLException e)
      { 
        // e.printStackTrace(); 
        // rollbackTransaction();
        return "Login failed\n";
      } 
            
  }


  /**
   * Implement the search function.
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
    return transaction_search_unsafe(originCity, destinationCity, directFlight, dayOfMonth, numberOfItineraries);
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
  private String transaction_search_unsafe(String originCity, String destinationCity, boolean directFlight,
                                          int dayOfMonth, int numberOfItineraries)
  { 
    ArrayList<ArrayList<Integer>> search_results = new ArrayList<ArrayList<Integer>>();   
    try 
    {
      beginTransaction();
      if (directFlight) 
      {
        search_results.addAll(get_direct_flights(originCity, destinationCity, directFlight, dayOfMonth, numberOfItineraries));
      }
      else 
      {
        search_results.addAll(get_direct_flights(originCity, destinationCity, directFlight, dayOfMonth, numberOfItineraries));
        int numDirects = search_results.size();
        int numIndirects = numberOfItineraries - numDirects;
        System.out.println(numberOfItineraries);
        System.out.println(numDirects);
        System.out.println(numIndirects);
        search_results.addAll(get_indirect_flights(originCity, destinationCity, directFlight, dayOfMonth, numIndirects));
      }
      for (int i=0; i<search_results.size(); i++) {
        ArrayList<Integer> result = search_results.get(i);
        String username = "test";
        int iid = 99;
        int numFlights = result.get(0);
        int fid_a = result.get(1);
        int fid_b = result.get(2);
        int totalTime = result.get(3);
        System.out.println("Itinerary " + iid + ": " + numFlights + " flight(s), " + totalTime + " minutes\n");

        // ADD THE ITINERARIES TO THE ITINERARIES DB after the search is executed
        // Include the user who ran the search
      }
      commitTransaction();
      return "End of list\n";
    }
    catch (SQLException e)
    {
      e.printStackTrace(); 
      return "Failed to search\n";
    }

  }

  private ArrayList<ArrayList<Integer>> get_direct_flights(String originCity, String destinationCity, boolean directFlight,
                                    int dayOfMonth, int numberOfItineraries) throws SQLException
  {
    ArrayList<ArrayList<Integer>> result = new ArrayList<ArrayList<Integer>>();    
    String directSearchSQL =
      "SELECT TOP (" + numberOfItineraries + ") fid as fid_a,actual_time "
      + "FROM Flights "
      + "WHERE origin_city = \'" + originCity + "\' AND dest_city = \'" + destinationCity + "\' AND day_of_month =  " + dayOfMonth + " "
      + "ORDER BY actual_time ASC, fid_a ASC";

    Statement searchStatement = conn.createStatement();
    ResultSet oneHopResults = searchStatement.executeQuery(directSearchSQL);

    while (oneHopResults.next())
    {
      int result_fid_a = oneHopResults.getInt("fid_a");
      int result_time = oneHopResults.getInt("actual_time");
      ArrayList<Integer> temp = new ArrayList<Integer>();
      temp.add(1);
      temp.add(result_fid_a);
      temp.add(0);
      temp.add(result_time);
      result.add(temp);
    }
    oneHopResults.close();
    return result;
  }

  private ArrayList<ArrayList<Integer>> get_indirect_flights(String originCity, String destinationCity, boolean directFlight,
                                      int dayOfMonth, int numberOfItineraries) throws SQLException
  {
    ArrayList<ArrayList<Integer>> result = new ArrayList<ArrayList<Integer>>();  
    String indirectSearchSQL =
      "SELECT DISTINCT TOP (" + numberOfItineraries + ") f1.fid as fid_a, f2.fid as fid_b, (f1.actual_time + f2.actual_time) as actual_time "
      + "FROM Flights as f1, Flights as f2 "
      + "WHERE f2.dest_city NOT IN "
      + "(SELECT DISTINCT(f0.dest_city) FROM FLIGHTS as f0 WHERE f0.origin_city = \'" + originCity + "\') "
      + "AND f1.origin_city = \'" + originCity + "\' "
      + "AND f1.dest_city = f2.origin_city "
      + "AND f2.dest_city != \'" + originCity + "\' "
      + "AND f2.dest_city = \'" + destinationCity + "\' AND f2.day_of_month =  " + dayOfMonth + " "
      + "ORDER BY actual_time ASC, fid_a ASC, fid_b ASC";

    Statement searchStatement = conn.createStatement();
    ResultSet indirectResults = searchStatement.executeQuery(indirectSearchSQL);
    while (indirectResults.next())
    {
      int result_fid_a = indirectResults.getInt("fid_a");
      int result_fid_b = indirectResults.getInt("fid_b");
      int result_time = indirectResults.getInt("actual_time");
      ArrayList<Integer> temp = new ArrayList<Integer>();
      temp.add(2);
      temp.add(result_fid_a);
      temp.add(result_fid_b);
      temp.add(result_time);
      result.add(temp);
    }
    indirectResults.close();
    return result;
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
        // if user loggedin = 0
          return "Cannot book reservations, not logged in\n"
        // else if itinerary id not in table
          return "No such itinerary {@code itineraryId}\n"
        // else if reservations table contains existing reservation for user for same day as 
        // the reservation with this itinerary id
          return "You cannot book two flights in the same day\n"
        // check if flight is full.. OR DO THIS ELSEWHERE
        // generate reservation ID (get max ID from reservation ID table and increment by 1)
        // add reservation with info from itinerary to DB
        // remove itinerary from itinerary DB
        commitTransaction();

      } 
      catch (SQLException e) 
      { 
        rollbackTransaction();
        return "Booking failed\n";
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

  // set reservation status to paid if user has enough money


  public String transaction_pay (int reservationId)
  {
    try
      {
        beginTransaction();
        // if user loggedin = 0
          return "Cannot pay, not logged in\n"
        // if reservationId not found in reservations table
          return "Cannot find unpaid reservation [reservationId] under user: [username]\n"
        // get price of reservation (may need to make some joins) as cost
        // get user balance
        // remaining = user_balance - cost
        // if remaining < 0 
          return "User has only [balance] in account but itinerary costs [cost]\n"
        // set reservation as paid
        // set user balance to remaining
        commitTransaction();
        balance = remaining
        return "Paid reservation: [reservationId] remaining balance: [balance]\n"
      }
      catch (SQLException e) 
      { 
        rollbackTransaction();
        return "Failed to pay for reservation " + reservationId + "\n";
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
  
  // list all active reservations for the logged in user 

  public String transaction_reservations()
  {
    try
      {
        beginTransaction();
        // if user loggedin = 0
          return "Cannot view reservations, not logged in\n"
        // if username not in reservations table
          return "No reservations found\n"
        // select reservations for user
          // return nicely printed reservation details
        commitTransaction();
      }
      catch (SQLException e) 
      { 
        rollbackTransaction();
        return "Failed to retrieve reservations\n";
      }
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

  // add reservation ID table

  public String transaction_cancel(int reservationId)
  { 
    try 
    {
        beginTransaction();
        // if user loggedin = 0
          return "Cannot cancel reservations, not logged in\n"
        // remove reservation from reservation table
        return "Canceled reservation " + reservationId;
        commitTransaction();
    }
    catch (SQLException e) 
    { 
        rollbackTransaction();
        return "Failed to cancel reservation " + reservationId;
    }
  }


  // DO I NEED TO MODIFY QUIT TO LOG OUT A USER

















        












