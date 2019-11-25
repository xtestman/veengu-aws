package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.BaseService;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.TaskDefinition;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;

import java.util.Map;

import static java.lang.String.valueOf;
import static software.amazon.awscdk.core.Duration.minutes;

public class FargateCluster extends Stack {

    private static final String AMAZON_LINUX = "arn:aws:ecr:us-west-2:137112412989:repository/amazonlinux";

    private BaseService service;

    private ApplicationLoadBalancer loadBalancer;

    private TaskDefinition taskDefinition;

    public FargateCluster(final Construct scope,
                          final String id,
                          final int containerPort) {
        super(scope, id);

        ///////////////////////////////////////////////////////////////////////////
        // VPC & Cluster
        ///////////////////////////////////////////////////////////////////////////

        Vpc vpc = Vpc.Builder
                .create(this, "Vpc")
                .build();

        Cluster cluster = Cluster.Builder
                .create(this, "FargateCluster")
                .vpc(vpc)
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Task Image
        ///////////////////////////////////////////////////////////////////////////

        // This is a workaround. The Amazon Linux container will be replaced by our container after the first execution of CodePipeline.
        IRepository defaultRegistry = Repository.fromRepositoryArn(this, "DefaultRegistry", AMAZON_LINUX);

        ContainerImage defaultImage = ContainerImage.fromEcrRepository(defaultRegistry);

        Map<String, String> environmentVariables = Map.of(
                "CONTAINER_PORT", valueOf(containerPort));

        ApplicationLoadBalancedTaskImageOptions taskImage = ApplicationLoadBalancedTaskImageOptions.builder()
                .image(defaultImage)
                .containerPort(containerPort)
                .environment(environmentVariables)
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Fargate Service
        ///////////////////////////////////////////////////////////////////////////

        ApplicationLoadBalancedFargateService fargateService = ApplicationLoadBalancedFargateService.Builder
                .create(this, "FargateService")
                .cluster(cluster)
                .taskImageOptions(taskImage)
                .desiredCount(1)
                .cpu(256)
                .memoryLimitMiB(512)
                .healthCheckGracePeriod(minutes(1))
                .build();

        this.service = fargateService.getService();
        this.loadBalancer = fargateService.getLoadBalancer();
        this.taskDefinition = fargateService.getTaskDefinition();
    }

    public BaseService getService() {
        return service;
    }

    public ApplicationLoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    public TaskDefinition getTaskDefinition() {
        return taskDefinition;
    }
}
