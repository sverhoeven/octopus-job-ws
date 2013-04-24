package nl.esciencecenter.octopus.webservice.resources;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import nl.esciencecenter.octopus.jobs.Job;
import nl.esciencecenter.octopus.webservice.api.JobSubmitRequest;
import nl.esciencecenter.octopus.webservice.api.JobSubmitResponse;
import nl.esciencecenter.octopus.webservice.job.OctopusManager;

import org.apache.http.client.HttpClient;

import com.yammer.metrics.annotation.Timed;


@Path("/jobs")
public class JobsResource {
    /**
     * Broker to submit jobs with
     */
    private final OctopusManager octopusmanager;
    /**
     * Http client to perform status callbacks with
     */
    private final HttpClient httpClient;

    /**
     * Constructor
     *
     * @param broker
     * @param httpClient
     */
    public JobsResource(OctopusManager octopusmanager, HttpClient httpClient) {
        this.octopusmanager = octopusmanager;
        this.httpClient = httpClient;
    }

    /**
     * Launch a job based on a request.
     *
     * @param request A job submission request
     * @return a job submission response
     * @throws Exception
     * @throws GATInvocationException
     * @throws GATObjectCreationException
     */
    @POST
    @Timed
    public JobSubmitResponse submitJob(JobSubmitRequest request) throws Exception {
        Job job = octopusmanager.submitJob(request, httpClient);

        return new JobSubmitResponse(job.getIdentifier());
    }
}