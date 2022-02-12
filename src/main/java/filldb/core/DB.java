package filldb.core;

import com.mysql.cj.jdbc.Driver;
import filldb.model.CliArguments;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public enum DB {
    ;

    public static Properties toDbDriverProperties(final CliArguments arguments) {
        final Properties props = new Properties();
        props.setProperty("user", arguments.username);
        props.setProperty("password", arguments.password);
        return props;
    }

    public static Connection connect(final String jdbcUrl, final Properties properties) throws SQLException {
        return new Driver().connect(jdbcUrl, properties);
    }

    public static void executeQueries(final Connection connection, final List<String> queries) throws SQLException {
        boolean error = false;
        Exception err = null;
        if (queries == null || queries.isEmpty()) {
            return;
        }
        Iterator<String> it = queries.iterator();
        while (it.hasNext()) {
            String query = it.next();
            if (query.isEmpty()) continue;
            try (final var statement = connection.createStatement()) {
                try {
                    statement.executeUpdate(query);
                } catch (Exception e) {
                    System.err.println("Error in Query: " + query + " " + e.toString());
                    err = e;
                    error = true;
                }
            }
        }
        if (error && null != err && err.toString().contains("REFERENCES")) {
            int indexRef = err.toString().indexOf("REFERENCES");
            String[] arr = err.toString().substring(indexRef).replace("REFERENCES", "").split(" ");
            if (arr.length >= 1) {
                String query = queries.stream().filter(q -> q.contains(arr[1])).findAny().orElse("");
                queries.remove(query);
                if (!queries.isEmpty()) {
                    queries.set(0, query);
                    executeQueries(connection, queries);
                    return;
                }
                if (!query.isEmpty()) {
                    List<String> nal = new ArrayList<>();
                    nal.add(query);
                    executeQueries(connection, nal);
                }
            }

        }
    }

}
