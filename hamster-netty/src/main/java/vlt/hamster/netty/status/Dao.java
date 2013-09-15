package vlt.hamster.netty.status;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;

/**
 * Handles all database routine.
 * 
 * @author vlt
 * 
 */
public class Dao {
    /**
     * Number of request that will be pooled from database and shown on status
     * page.
     */
    public static final int NUMBER_OF_REQUESTS_OUTPUT = 100;
    private static final String MERGE_SERVER_REQUEST_RECORD_STRING = "MERGE INTO server_request s "
	    + "USING (VALUES (NULL, ?, ?, 0)) AS vals(n,x,y,z) ON s.source_ip = vals.x "
	    + "WHEN MATCHED THEN UPDATE SET s.request_time = vals.y, s.request_count = s.request_count+1 "
	    + "WHEN NOT MATCHED THEN INSERT VALUES (DEFAULT, vals.x, vals.y, 1)";
    private static final String MERGE_REDIRECT_REQUEST_RECORD_STRING = "MERGE INTO redirect_request r "
	    + "USING (VALUES (NULL, ?, 0)) AS vals(n,x,y) ON r.redirect_url = vals.x "
	    + "WHEN MATCHED THEN UPDATE SET r.redirect_count = r.redirect_count+1 "
	    + "WHEN NOT MATCHED THEN INSERT VALUES (DEFAULT, vals.x, DEFAULT)";
    private static final String GET_SERVER_REQUEST_RECORD_STRING = "SELECT top "
	    + NUMBER_OF_REQUESTS_OUTPUT
	    + " source_ip, request_count, request_time "
	    + "FROM server_request ORDER BY request_time DESC";
    private static final String GET_REDIRECT_REQUEST_RECORD_STRING = "SELECT top "
	    + NUMBER_OF_REQUESTS_OUTPUT
	    + " redirect_url, redirect_count "
	    + "FROM redirect_request ORDER BY redirect_count DESC";
    private static ComboPooledDataSource cpds;
    static {
	try {
	    cpds = new ComboPooledDataSource();
	    /*
	     * load the jdbc driver and configure it
	     */
	    cpds.setDriverClass("org.hsqldb.jdbc.JDBCDriver");
	    cpds.setJdbcUrl("jdbc:hsqldb:file:db/hamsterDB");
	    cpds.setUser("SA");
	    cpds.setPassword("123");
	    cpds.setMaxStatements(100);
	} catch (PropertyVetoException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public static Connection getConnection() throws SQLException {
	Connection c = cpds.getConnection();
	return c;
    }

    /**
     * Insert server request record to database if record for Source IP is not
     * there yet. Else updates existing record.
     * 
     * @param record
     * @throws SQLException
     */
    public void mergeServerRequestRecord(ServerRequestRecord record)
	    throws SQLException {
	Connection connection = getConnection();
	PreparedStatement statement = connection
		.prepareStatement(MERGE_SERVER_REQUEST_RECORD_STRING);
	statement.setString(1, record.getSourceIp());
	statement.setTimestamp(2, record.getLastRequest());
	statement.executeUpdate();
	connection.close();
    }

    /**
     * 
     * @return List of {@link vlt.hamster.netty.status.ServerRequestRecord
     *         ServerRequestRecord} from database. Number of returned records is
     *         determined by
     *         {@link vlt.hamster.netty.status.Dao.NUMBER_OF_REQUESTS_OUTPUT
     *         NUMBER_OF_REQUESTS_OUTPUT}.
     * @throws SQLException
     */
    public List<ServerRequestRecord> getServerRequest() throws SQLException {
	List<ServerRequestRecord> resultList = new ArrayList<>();
	Connection connection = getConnection();
	Statement statement = connection.createStatement();
	ResultSet rs = statement.executeQuery(GET_SERVER_REQUEST_RECORD_STRING);
	while (rs.next()) {
	    ServerRequestRecord record = new ServerRequestRecord(
		    rs.getString("source_ip"));
	    record.setLastRequest(rs.getTimestamp("request_time"));
	    record.setRequestCount(rs.getLong("request_count"));
	    resultList.add(record);
	}
	connection.close();
	return resultList;
    }

    /**
     * 
     * @return number of server requests
     * @throws SQLException
     */
    public long getServerRequestcount() throws SQLException {
	Connection connection = getConnection();
	Statement st = connection.createStatement();
	ResultSet rs = st
		.executeQuery("SELECT SUM(request_count) as n  FROM server_request");
	rs.next();
	long result = rs.getLong("n");
	connection.close();
	return result;
    }

    /**
     * 
     * @return Number of unique server request.
     * @throws SQLException
     */
    public long getServerRequestUniqueCount() throws SQLException {
	Connection connection = getConnection();
	Statement st = connection.createStatement();
	ResultSet rs = st
		.executeQuery("SELECT COUNT(*) as n  FROM server_request");
	rs.next();
	long result = rs.getLong("n");
	connection.close();
	return result;
    }

    /**
     * Inserts redirect request record to database if record for URL is not
     * there yet. Else updates existing record.
     * 
     * @param record
     * @throws SQLException
     */
    public void mergeRedirectRequestRecord(RedirectRequestRecord record)
	    throws SQLException {
	Connection connection = getConnection();
	PreparedStatement statement = connection
		.prepareStatement(MERGE_REDIRECT_REQUEST_RECORD_STRING);
	statement.setString(1, record.getRedirectUrl().toString());
	statement.executeUpdate();
	connection.close();
    }

    /**
     * 
     * @return List of {@link vlt.hamster.netty.status.RedirectRequestRecord
     *         RedirectRequestRecord} from database. Number of returned records
     *         is determined by
     *         {@link vlt.hamster.netty.status.Dao.NUMBER_OF_REQUESTS_OUTPUT
     *         NUMBER_OF_REQUESTS_OUTPUT}.
     * @throws SQLException
     */
    public List<RedirectRequestRecord> getRedirectRequest() throws SQLException {
	List<RedirectRequestRecord> resultList = new ArrayList<>();
	Connection connection = getConnection();
	Statement statement = connection.createStatement();
	ResultSet rs = statement
		.executeQuery(GET_REDIRECT_REQUEST_RECORD_STRING);
	while (rs.next()) {
	    RedirectRequestRecord record = new RedirectRequestRecord();
	    record.setRedirectCount(rs.getLong("redirect_count"));
	    record.setRedirectUrl(rs.getString("redirect_url"));
	    resultList.add(record);
	}
	connection.close();
	return resultList;
    }

    /**
     * Shuts down the HSQLDB instance correctly.
     * 
     * @throws SQLException
     */
    public static void shutdownDb() throws SQLException {
	Connection connection = getConnection();
	Statement statement = connection.createStatement();
	statement.execute("SHUTDOWN");
	connection.close();
	DataSources.destroy(cpds);
    }
}
