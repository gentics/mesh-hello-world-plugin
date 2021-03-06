package com.gentics.mesh.plugin;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.graphql.GraphQLPlugin;
import com.gentics.mesh.plugin.graphql.GraphQLPluginContext;

import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLSchema.Builder;
import io.reactivex.Completable;

public class GraphQLExamplePlugin extends AbstractPlugin implements GraphQLPlugin {

	private GraphQLSchema schema;

	public GraphQLExamplePlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Completable initialize() {
		Builder schemaBuilder = GraphQLSchema.newSchema();
		schema = schemaBuilder.query(newObject()
			.name(prefixType("PluginDataType"))
			.description("Dummy GraphQL Test")
			.field(newFieldDefinition().name("text")
				.type(GraphQLString)
				.description("Say hello to the world of plugins")
				.dataFetcher(env -> {
					GraphQLPluginContext ctx = env.getContext();
					// We can check for which project the query was executed
					System.out.println("Project Name: " + ctx.projectName());
					System.out.println("Project Uuid: " + ctx.projectUuid());
					// We can also access the user
					System.out.println("User: " + ctx.principal().encodePrettily());
					return "hello-world";
				}))
			.build()).build();
		return Completable.complete();
	}

	@Override
	public GraphQLSchema createRootSchema() {
		return schema;
	}

}
