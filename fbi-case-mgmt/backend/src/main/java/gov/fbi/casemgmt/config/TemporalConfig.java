package gov.fbi.casemgmt.config;

import gov.fbi.casemgmt.workflow.activity.CaseLifecycleActivitiesImpl;
import gov.fbi.casemgmt.workflow.activity.DocumentIngestActivitiesImpl;
import gov.fbi.casemgmt.workflow.impl.CaseLifecycleWorkflowImpl;
import gov.fbi.casemgmt.workflow.impl.DocumentIngestWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Temporal wiring. Split into:
 *   (1) client/stubs/factory as @Bean beans, and
 *   (2) a separate {@link WorkerRegistrar} component that starts workers
 *       after the application is fully constructed — this avoids any circular
 *       dependency between Spring and Temporal activity beans.
 */
@Configuration
public class TemporalConfig {

    public static final String CASE_LIFECYCLE_QUEUE  = "case-lifecycle-queue";
    public static final String DOCUMENT_INGEST_QUEUE = "document-ingest-queue";

    @Bean
    WorkflowServiceStubs workflowServiceStubs(
            @Value("${temporal.connection.target}") String target) {
        return WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder().setTarget(target).build());
    }

    @Bean
    WorkflowClient workflowClient(WorkflowServiceStubs stubs,
                                  @Value("${temporal.namespace}") String namespace) {
        return WorkflowClient.newInstance(stubs,
            WorkflowClientOptions.newBuilder().setNamespace(namespace).build());
    }

    @Bean
    WorkerFactory workerFactory(WorkflowClient client) {
        return WorkerFactory.newInstance(client);
    }

    @Component
    @RequiredArgsConstructor
    @Slf4j
    public static class WorkerRegistrar implements ApplicationListener<ApplicationReadyEvent> {

        private final WorkerFactory workerFactory;
        private final CaseLifecycleActivitiesImpl caseActivities;
        private final DocumentIngestActivitiesImpl ingestActivities;

        @Override
        public void onApplicationEvent(ApplicationReadyEvent event) {
            Worker caseWorker = workerFactory.newWorker(CASE_LIFECYCLE_QUEUE);
            caseWorker.registerWorkflowImplementationTypes(CaseLifecycleWorkflowImpl.class);
            caseWorker.registerActivitiesImplementations(caseActivities);

            Worker ingestWorker = workerFactory.newWorker(DOCUMENT_INGEST_QUEUE);
            ingestWorker.registerWorkflowImplementationTypes(DocumentIngestWorkflowImpl.class);
            ingestWorker.registerActivitiesImplementations(ingestActivities);

            workerFactory.start();
            log.info("Temporal workers started on queues: {}, {}",
                CASE_LIFECYCLE_QUEUE, DOCUMENT_INGEST_QUEUE);
        }
    }
}
