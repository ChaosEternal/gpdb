/*
 * Copyright (c) 2004, 2005, 2006 TADA AB - Taby Sweden
 * Distributed under the terms shown in the file COPYRIGHT
 * found in the root folder of this project or at
 * http://eng.tada.se/osprojects/COPYRIGHT.html
 */
package org.postgresql.pljava.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.ArrayList;

import org.postgresql.pljava.internal.ExecutionPlan;
import org.postgresql.pljava.internal.Portal;
import org.postgresql.pljava.internal.SPI;
import org.postgresql.pljava.internal.SPIException;

/**
 *
 * @author Thomas Hallgren
 */
public class SPIStatement implements Statement
{
	private final SPIConnection m_connection;
	
	// Default settings.
	//
	private String    m_cursorName     = null;
	private int       m_fetchSize      = 1000;
	private int       m_maxRows        = 0;
	private ResultSet m_resultSet      = null;
	private int       m_updateCount    = 0;
	private ArrayList m_batch          = null;
    private boolean   m_closed         = false;

	public SPIStatement(SPIConnection conn)
	{
		m_connection = conn;
	}

	public void addBatch(String statement)
	throws SQLException
	{
		// Statements are converted to native SQL once they
		// are executed.
		//
		this.internalAddBatch(statement);
	}

	public void cancel()
	throws SQLException
	{
	}

	public void clearBatch()
	throws SQLException
	{
		m_batch = null;
	}

	public void clearWarnings()
	throws SQLException
	{
	}
		
	public void close()
	throws SQLException
	{
		if(m_resultSet != null)
		{
			//
			// The close will call back to the resultSetClosed method
			// and set the m_resultSet to null.
			//
			m_resultSet.close();
		}
		m_updateCount = -1;
		m_cursorName = null;
		m_batch = null;
        m_closed = true;
	}
	
	/**
	 * Retrieves whether this Statement object has been closed. A Statement is
	 * closed if the method close has been called on it, or if it is
	 * automatically closed.
	 *
	 * @return true if this Statement object is closed; false if it is still open 
	 * @throw SQLException - if a database access error occurs
	 * @since 1.6
	 */
	public boolean isClosed()
	throws SQLException
	{
		return m_closed;
	}

	/**
	 * Requests that a Statement be pooled or not pooled. The value specified is
	 * a hint to the statement pool implementation indicating whether the
	 * applicaiton wants the statement to be pooled. It is up to the statement
	 * pool manager as to whether the hint is used.
	 *
	 * The poolable value of a statement is applicable to both internal
	 * statement caches implemented by the driver and external statement caches
	 * implemented by application servers and other applications.
	 *
	 * By default, a Statement is not poolable when created, and a
	 * PreparedStatement and CallableStatement are poolable when created.
	 *
	 * @param poolable - requests that the statement be pooled if true and that
	 * the statement not be pooled if false
	 * @throw SQLException - if this method is called on a closed Statement
	 * @since 1.6
	 */
	public void setPoolable(boolean poolable)
	throws SQLException
	{
		throw new UnsupportedFeatureException("Statement.setPoolable");
	}

	/**
	 * Returns a value indicating whether the Statement is poolable or not.
	 * 
	 * @return true if the Statement is poolable; false otherwise
	 * @throw SQLException - if this method is called on a closed Statement
	 * @since 1.6
	 */
	public boolean isPoolable()
    throws SQLException
	{
		return false;
	}


	public boolean execute(String statement)
	throws SQLException
	{
		// Ensure that the statement is closed before we re-execute
		//
		this.close();

		ExecutionPlan plan = ExecutionPlan.prepare(
			m_connection.nativeSQL(statement), null);

		int result = SPI.getResult();
		if(plan == null)
			throw new SPIException(result);

		try
		{
			return this.executePlan(plan, null);
		}
		finally
		{
			try { plan.close(); } catch(Exception e) {}
 		}
	}

	protected boolean executePlan(ExecutionPlan plan, Object[] paramValues)
	throws SQLException
	{
		m_updateCount = -1;
		m_resultSet   = null;

		boolean isResultSet = plan.isCursorPlan();
		if(isResultSet)
		{
			Portal portal = plan.cursorOpen(m_cursorName, paramValues);
			m_resultSet = new SPIResultSet(this, portal, m_maxRows);
		}
		else
		{
			try
			{
				plan.execute(paramValues, m_maxRows);
				m_updateCount = SPI.getProcessed();
			}
			finally
			{
				SPI.freeTupTable();
			}
		}
		return isResultSet;
	}

	/**
	 * Return of auto generated keys is not yet supported.
	 * @throws SQLException indicating that this feature is not supported.
	 */
	public boolean execute(String statement, int autoGeneratedKeys)
	throws SQLException
	{
		throw new UnsupportedFeatureException("Statement.execute(String,int)");
	}

	/**
	 * Return of auto generated keys is not yet supported.
	 * @throws SQLException indicating that this feature is not supported.
	 */
	public boolean execute(String statement, int[] columnIndexes)
	throws SQLException
	{
		throw new UnsupportedFeatureException("Statement.execute(String,int[])");
	}

	/**
	 * Return of auto generated keys is not yet supported.
	 * @throws SQLException indicating that this feature is not supported.
	 */
	public boolean execute(String statement, String[] columnNames)
	throws SQLException
	{
		throw new UnsupportedFeatureException("Statement.execute(String,String[])");
	}

	public int[] executeBatch()
	throws SQLException
	{
		int numBatches = (m_batch == null) ? 0 : m_batch.size();
		int[] result = new int[numBatches];
		for(int idx = 0; idx < numBatches; ++idx)
			result[idx] = this.executeBatchEntry(m_batch.get(idx));
		return result;
	}

	public ResultSet executeQuery(String statement)
	throws SQLException
	{
		this.execute(statement);
		return this.getResultSet();
	}

	public int executeUpdate(String statement)
	throws SQLException
	{
		this.execute(statement);
		return this.getUpdateCount();
	}

	/**
	 * Return of auto generated keys is not yet supported.
	 * @throws SQLException indicating that this feature is not supported.
	 */
	public int executeUpdate(String statement, int autoGeneratedKeys)
	throws SQLException
	{
		throw new UnsupportedFeatureException("Auto generated key support not yet implemented");
	}


	/**
	 * Return of auto generated keys is not yet supported.
	 * @throws SQLException indicating that this feature is not supported.
	 */
	public int executeUpdate(String statement, int[] columnIndexes)
	throws SQLException
	{
		throw new UnsupportedFeatureException("Auto generated key support not yet implemented");
	}


	/**
	 * Return of auto generated keys is not yet supported.
	 * @throws SQLException indicating that this feature is not supported.
	 */
	public int executeUpdate(String statement, String[] columnNames)
	throws SQLException
	{
		throw new UnsupportedFeatureException("Auto generated key support not yet implemented");
	}

	/**
	 * Returns the Connection from that created this statement.
	 * @throws SQLException if the statement is closed.
	 */
	public Connection getConnection()
	throws SQLException
	{
		if(m_connection == null)
			throw new StatementClosedException();
		return m_connection;
	}

	public int getFetchDirection()
	throws SQLException
	{
		return ResultSet.FETCH_FORWARD;
	}
	
	public int getFetchSize()
	throws SQLException
	{
		return m_fetchSize;
	}
	
	public ResultSet getGeneratedKeys()
	throws SQLException
	{
		throw new SQLException("JDK 1.4 functionality not yet implemented");
	}
	
	public int getMaxFieldSize()
	throws SQLException
	{
		return Integer.MAX_VALUE;
	}
	
	public int getMaxRows()
	throws SQLException
	{
		return m_maxRows;
	}
	
	public boolean getMoreResults()
	throws SQLException
	{
		return false;
	}
	
	public boolean getMoreResults(int current)
	throws SQLException
	{
		return false;
	}
	
	public int getQueryTimeout()
	throws SQLException
	{
		return 0;
	}
	
	public ResultSet getResultSet()
	throws SQLException
	{
		return m_resultSet;
	}

	public int getResultSetConcurrency()
	{
		return ResultSet.CONCUR_READ_ONLY;
	}

	public int getResultSetHoldability()
	throws SQLException
	{
		throw new SQLException("JDK 1.4 functionality not yet implemented");
	}

	public int getResultSetType()
	{
		return ResultSet.TYPE_FORWARD_ONLY;
	}

	public int getUpdateCount()
	throws SQLException
	{
		return m_updateCount;
	}

	public SQLWarning getWarnings()
	throws SQLException
	{
        if (m_closed) {
            throw new SQLException("getWarnings: Statement is closed");
        }

		return null;
	}

	public void setCursorName(String cursorName)
	throws SQLException
	{
		m_cursorName = cursorName;
	}

	public void setEscapeProcessing(boolean enable)
	throws SQLException
	{
		throw new UnsupportedFeatureException("Statement.setEscapeProcessing");
	}


	/**
	 * Only {@link ResultSet#FETCH_FORWARD} is supported.
	 * @throws SQLException indicating that this feature is not supported
	 * for other values on <code>direction</code>.
	 */
	public void setFetchDirection(int direction)
	throws SQLException
	{
		if(direction != ResultSet.FETCH_FORWARD)
			throw new UnsupportedFeatureException("Non forward fetch direction");
	}

	public void setFetchSize(int size)
	throws SQLException
	{
		m_fetchSize = size;
	}

	public void setMaxFieldSize(int size)
	throws SQLException
	{
		throw new UnsupportedFeatureException("Statement.setMaxFieldSize");
	}

	public void setMaxRows(int rows)
	throws SQLException
	{
		m_maxRows = rows;
	}

	public void setQueryTimeout(int seconds)
	throws SQLException
	{
		throw new UnsupportedFeatureException("Statement.setQueryTimeout");
	}

	protected void internalAddBatch(Object batch)
	throws SQLException
	{
		if(m_batch == null)
			m_batch = new ArrayList();
		m_batch.add(batch);
	}

	protected int executeBatchEntry(Object batchEntry)
	throws SQLException
	{
		int ret = SUCCESS_NO_INFO;
		if(this.execute(m_connection.nativeSQL((String)batchEntry)))
			this.getResultSet().close();
		else if(m_updateCount >= 0)
			ret = m_updateCount;
		return ret;
	}

	void resultSetClosed(ResultSet rs)
	{
		if(rs == m_resultSet)
			m_resultSet = null;
	}

	/**
	 * Returns true if this either implements the interface argument or is
	 * directly or indirectly a wrapper for an object that does. Returns false
	 * otherwise. If this implements the interface then return true, else if
	 * this is a wrapper then return the result of recursively calling
	 * isWrapperFor on the wrapped object. If this does not implement the
	 * interface and is not a wrapper, return false. This method should be
	 * implemented as a low-cost operation compared to unwrap so that callers
	 * can use this method to avoid expensive unwrap calls that may fail. If
	 * this method returns true then calling unwrap with the same argument
	 * should succeed.
	 *
	 * @since 1.6
	 */
	public boolean isWrapperFor(Class iface)
	throws SQLException
	{
		throw new UnsupportedFeatureException("isWrapperFor");
	}

	/**
	 * Returns an object that implements the given interface to allow access to
	 * non-standard methods, or standard methods not exposed by the proxy. If
	 * the receiver implements the interface then the result is the receiver or
	 * a proxy for the receiver. If the receiver is a wrapper and the wrapped
	 * object implements the interface then the result is the wrapped object or
	 * a proxy for the wrapped object. Otherwise return the the result of
	 * calling unwrap recursively on the wrapped object or a proxy for that
	 * result. If the receiver is not a wrapper and does not implement the
	 * interface, then an SQLException is thrown.
	 *
	 * @since 1.6
	 */
	public Object unwrap(Class iface)
	throws SQLException
	{
		throw new UnsupportedFeatureException("unwrap");
	}
}