package org.hypergraphql;

import com.fasterxml.jackson.databind.JsonNode;
import graphql.*;
import graphql.execution.NonNullableFieldWasNullError;
import graphql.schema.idl.errors.NotAnOutputTypeError;
import graphql.validation.ValidationError;
import graphql.validation.ValidationErrorType;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by szymon on 01/11/2017.
 */
public class GraphqlService {
    private GraphQL graphQL;
    private Config config;
    static Logger logger = Logger.getLogger(GraphqlService.class);


    public GraphqlService(Config config, GraphQL graphQL) {
        this.graphQL = graphQL;
        this.config = config;
    }

    public Map<String, Object> results(String query) {

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        Map<Object, Object> extensions = new HashMap<>();
        List<GraphQLError> errors = new ArrayList<>();

        ExecutionInput executionInput;
        ExecutionResult qlResult;

        List<Map<String, String>> sparqlQueries;

        if (!query.contains("IntrospectionQuery") && !query.contains("__")) {

            Converter converter = new Converter(config);
            JsonNode jsonQuery;
            try {

                jsonQuery = converter.query2json(query);

            } catch (Exception e) {

                System.out.println("Houston...");
                GraphQLError err = new ValidationError(ValidationErrorType.InvalidSyntax);
                errors.add(err);
                result.put("errors", errors);
                return result;

            }

            sparqlQueries = converter.graphql2sparql(converter.includeContextInQuery(jsonQuery));


            // uncomment this lines if you want to include the generated SPARQL queries in the GraphQL response for debugging purposes
            // extensions.put("sparqlQueries", sparqlQueries);

            logger.info("Generated SPARQL queries:");
            logger.info(sparqlQueries.toString());

            SparqlClient client = new SparqlClient(sparqlQueries, config);

            executionInput = ExecutionInput.newExecutionInput()
                    .query(query)
                    .context(client)
                    .build();

            qlResult = graphQL.execute(executionInput);
            try {
                data = converter.jsonLDdata(qlResult.getData(), jsonQuery);
            } catch (IOException e) {
                logger.error(e);
            }

        } else {
            qlResult = graphQL.execute(query);
            data = qlResult.getData();
        }

        errors.addAll(qlResult.getErrors());

        result.put("data", data);
        result.put("errors", errors);
        result.put("extensions", extensions);

        return result;
    }
}
