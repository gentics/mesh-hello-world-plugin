package com.gentics.mesh.plugin;

import java.io.File;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.reactivex.core.buffer.Buffer;

public class HelloWorldPlugin extends AbstractPlugin implements RestPlugin {

	private static final Logger log = LoggerFactory.getLogger(HelloWorldPlugin.class);

	public static final String PROJECT_NAME = "HelloWorld";

	public HelloWorldPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Completable initialize() {
		Single<HelloConfig> rxConfig = Single.fromCallable(() -> {
			if (!getConfigFile().exists()) {
				HelloConfig config = new HelloConfig().setName("hello-world");
				return writeConfig(config);
			} else {
				return readConfig(HelloConfig.class);
			}
		}).doOnSuccess(config -> {
			log.info("Loaded config {\n" + PluginConfigUtil.getYAMLMapper().writeValueAsString(config) + "\n}");
		});

		// The initialize method can be used to setup initial data which is needed by the plugin.
		// You can use the admin client to setup initial data or access the filesystem to read/write data.
		String path = new File(getStorageDir(), "dummyFile.txt").getAbsolutePath();
		return rxConfig.ignoreElement().andThen(getRxVertx().fileSystem()
			.rxWriteFile(path, Buffer.buffer("test"))
			.andThen(createProject()));
	}

	/**
	 * Utilize the admin client and create a project.
	 * 
	 * @return
	 */
	private Completable createProject() {
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(PROJECT_NAME);
		request.setSchemaRef("folder");
		MeshRestClient client = adminClient();
		return client.createProject(request).toCompletable();
	}

	@Override
	public Router createGlobalRouter() {
		Router router = Router.router(vertx());
		// Route which will use the admin client to load the previously created project and return it.
		// Path: /api/v1/plugins/hello-world/project
		router.route("/project").handler(rc -> {
			PluginContext context = wrap(rc);
			adminClient().findProjectByName(PROJECT_NAME).toSingle().subscribe(project -> {
				context.send(project, 200);
			}, rc::fail);
		});

		// Route to serve static contents from the webroot resources folder of the plugin.
		// Path: /api/v1/plugins/hello-world/static
		StaticHandler staticHandler = StaticHandler.create("webroot-hello", getClass().getClassLoader());
		router.route("/static/*").handler(staticHandler);

		// Route which will return the user information
		// Path: /api/v1/plugins/hello-world/me
		router.route("/me").handler(rc -> {
			PluginContext context = wrap(rc);
			context.client().me().toSingle().subscribe(me -> {
				rc.response().putHeader("content-type", "application/json");
				rc.response().end(me.toJson());
			}, rc::fail);
		});

		// Route which demonstrates that the API can be directly extended
		// Path: /api/v1/plugins/hello-world/hello
		router.route("/hello").handler(rc -> {
			rc.response().end("world");
		});

		return router;
	}

	@Override
	public Router createProjectRouter() {
		Router router = Router.router(vertx());
		log.info("Registering routes for {" + id() + "}");

		// Route which demonstrates that plugins can also have project specific routes.
		// Path: /api/v1/:projectName/plugins/helloworld/hello
		// It is possible to access the project information via the context project() method.
		router.route("/hello").handler(rc -> {
			PluginContext context = wrap(rc);
			rc.response().end("world-project-" + context.project().getString("name"));
		});
		return router;
	}

	@Override
	public String restApiName() {
		return "hello-world";
	}

}
