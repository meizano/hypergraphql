package org.hypergraphql;

public class EndPoint {

    public static void main(final String[] args) throws Exception {

        final String[] arguments = {
                "--classpath",
                "--config",
                "config.json"
        };

        Application.main(arguments);
//        Application.main(args);
    }
}
