package top.infra.maven.extension;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.ProjectBuildingRequest;

import top.infra.maven.core.CiOptionContext;
import top.infra.maven.extension.activator.model.ProjectBuilderActivatorModelResolver;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;

@Named
@Singleton
public class ModelResolverEventAware implements MavenEventAware {

    protected final Logger logger;

    protected final ProjectBuilderActivatorModelResolver resolver;

    @Inject
    public ModelResolverEventAware(
        final org.codehaus.plexus.logging.Logger logger,
        final ProjectBuilderActivatorModelResolver resolver
    ) {
        this.logger = new LoggerPlexusImpl(logger);
        this.resolver = resolver;
    }

    @Override
    public int getOrder() {
        return Orders.EVENT_AWARE_ORDER_MODEL_RESOLVER;
    }

    @Override
    public void onProjectBuildingRequest(
        final MavenExecutionRequest mavenExecution,
        final ProjectBuildingRequest projectBuilding,
        final CiOptionContext ciOptContext
    ) {
        this.resolver.setProjectBuildingRequest(projectBuilding);
    }
}
