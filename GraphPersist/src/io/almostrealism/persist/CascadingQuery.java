package io.almostrealism.persist;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;

import io.almostrealism.sql.SQLConnectionProvider;

public abstract class CascadingQuery<D extends SQLConnectionProvider, K, V extends Cacheable> extends CacheableQuery<D, K, V> {
	@Override
	public Collection<V> execute(D database, K key, List<CascadingQuery> cascades) {
		init(key);
		
		try (Statement s = database.getSQLConnection().createStatement()) {
			ResultSet rs = s.executeQuery(getQuery(key));
			
			while (rs.next()) {
				process(rs, key);
				for (CascadingQuery q : cascades) q.process(rs, key);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return getReturnValue(key);
	}
	
	public boolean isRoot() { return false; }
	
	public abstract void init(K key);
	
	public abstract String getQuery(K key);
	
	public abstract Collection<V> getReturnValue(K key);
	
	public abstract void process(ResultSet rs, K arguments) throws SQLException;
}
