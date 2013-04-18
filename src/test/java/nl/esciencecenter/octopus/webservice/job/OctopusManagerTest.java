package nl.esciencecenter.octopus.webservice.job;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import nl.esciencecenter.octopus.Octopus;
import nl.esciencecenter.octopus.OctopusFactory;
import nl.esciencecenter.octopus.exceptions.OctopusException;
import nl.esciencecenter.octopus.exceptions.OctopusIOException;
import nl.esciencecenter.octopus.files.AbsolutePath;
import nl.esciencecenter.octopus.files.FileSystem;
import nl.esciencecenter.octopus.files.Files;
import nl.esciencecenter.octopus.files.RelativePath;
import nl.esciencecenter.octopus.jobs.Job;
import nl.esciencecenter.octopus.jobs.JobDescription;
import nl.esciencecenter.octopus.jobs.JobStatus;
import nl.esciencecenter.octopus.jobs.Jobs;
import nl.esciencecenter.octopus.jobs.Scheduler;
import nl.esciencecenter.octopus.util.Sandbox;
import nl.esciencecenter.octopus.webservice.api.JobStatusResponse;
import nl.esciencecenter.octopus.webservice.api.JobSubmitRequest;

import org.apache.http.client.HttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableMap;

/**
 *
 * @author verhoes
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(OctopusFactory.class)
public class OctopusManagerTest {

    @Test
    public void testOctopusManager() throws URISyntaxException, OctopusException, OctopusIOException {
        // Use powermock mockito to mock OctopusFactory.newOctopus()
        PowerMockito.mockStatic(OctopusFactory.class);
        Octopus octopus = mock(Octopus.class);
        Jobs jobs = mock(Jobs.class);
        when(octopus.jobs()).thenReturn(jobs);
        when(OctopusFactory.newOctopus(any(Properties.class))).thenReturn(octopus);

        ImmutableMap<String, Object> prefs = ImmutableMap.of("octopus.adaptors.local.queue.multi.maxConcurrentJobs", (Object) 1);
        OctopusConfiguration conf =
                new OctopusConfiguration(new URI("local:///"), "multi", new URI("file:///tmp/sandboxes"), prefs);

        new OctopusManager(conf);

        // verify octopus created
        PowerMockito.verifyStatic();
        Properties expected_props = new Properties();
        expected_props.setProperty("octopus.adaptors.local.queue.multi.maxConcurrentJobs", "1");
        OctopusFactory.newOctopus(expected_props);
        // verify scheduler created
        when(jobs.newScheduler(new URI("local:///"), null, expected_props));
    }

    @Test
    public void testStart() throws Exception {
        JobsPoller poller = mock(JobsPoller.class);
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        OctopusConfiguration conf = new OctopusConfiguration();
        OctopusManager manager = new OctopusManager(conf, null, null, null, poller, executor);

        manager.start();

        verify(executor).scheduleAtFixedRate(poller, 0, 30*1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testStop() throws Exception {
        Octopus octopus = mock(Octopus.class);
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        OctopusManager manager = new OctopusManager(null, octopus, null, null, null, executor);

        manager.stop();

        verify(octopus).end();
        verify(executor).shutdown();
        verify(executor).awaitTermination(1, TimeUnit.MINUTES);
    }

    @Test
    public void testSubmitJob() throws OctopusIOException, OctopusException, URISyntaxException {
        Properties props = new Properties();
        props.put("octopus.adaptors.local.queue.multi.maxConcurrentJobs", "4");
        ImmutableMap<String, Object> prefs = ImmutableMap.of("octopus.adaptors.local.queue.multi.maxConcurrentJobs", (Object) 1);
        OctopusConfiguration conf =
                new OctopusConfiguration(new URI("local:///"), "multi", new URI("file:///tmp/sandboxes"), prefs);
        Octopus octopus = mock(Octopus.class);
        Scheduler scheduler = mock(Scheduler.class);
        Jobs jobs = mock(Jobs.class);
        when(octopus.jobs()).thenReturn(jobs);
        Files files = mock(Files.class);
        when(octopus.files()).thenReturn(files);
        AbsolutePath sandboxPath = mock(AbsolutePath.class);
        FileSystem filesystem = mock(FileSystem.class);
        when(files.newFileSystem(new URI("file:///"), null, null)).thenReturn(filesystem);
        when(files.newPath(filesystem, new RelativePath("/tmp/sandboxes"))).thenReturn(sandboxPath);
        JobSubmitRequest request = mock(JobSubmitRequest.class);
        JobDescription description = mock(JobDescription.class);
        when(request.toJobDescription(octopus)).thenReturn(description);
        Sandbox sandbox = mock(Sandbox.class);
        when(request.toSandbox(octopus, sandboxPath, null)).thenReturn(sandbox);
        when(sandbox.getPath()).thenReturn(sandboxPath);
        when(sandboxPath.getPath()).thenReturn("/tmp/sandboxes");
        HttpClient httpClient = mock(HttpClient.class);
        Job job = mock(Job.class);
        when(job.getIdentifier()).thenReturn("11111111-1111-1111-1111-111111111111");
        when(jobs.submitJob(scheduler, description)).thenReturn(job);
        Map<String, SandboxedJob> sjobs = new HashMap<String, SandboxedJob>();
        JobsPoller poller = mock(JobsPoller.class);
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        OctopusManager manager = new OctopusManager(conf, octopus, scheduler, sjobs, poller, executor);

        Job result = manager.submitJob(request, httpClient);

        assertThat(result.getIdentifier()).isEqualTo("11111111-1111-1111-1111-111111111111");
        verify(sandbox).upload();
        verify(jobs).submitJob(scheduler, description);
    }

    @Test
    public void testStateOfJob() throws URISyntaxException, OctopusIOException, OctopusException {
        // create manager with stubbed members
        Octopus octopus = mock(Octopus.class);
        Jobs jobsEngine= mock(Jobs.class);
        when(octopus.jobs()).thenReturn(jobsEngine);
        Map<String, SandboxedJob> sjobs = new HashMap<String, SandboxedJob>();
        SandboxedJob sjob = mock(SandboxedJob.class);
        Job job = mock(Job.class);
        when(sjob.getJob()).thenReturn(job);
        sjobs.put("11111111-1111-1111-1111-111111111111", sjob);
        OctopusManager manager = new OctopusManager(null, octopus, null, sjobs, null, null);
        // created jobstatus stub
        JobStatus jobstatus = mock(JobStatus.class);
        when(jobstatus.getState()).thenReturn("DONE");
        when(jobstatus.isDone()).thenReturn(true);
        when(jobstatus.getExitCode()).thenReturn(0);
        when(jobstatus.getException()).thenReturn(null);
        when(jobstatus.getSchedulerSpecficInformation()).thenReturn(null);
        when(jobsEngine.getJobStatus(job)).thenReturn(jobstatus);

        JobStatusResponse result = manager.stateOfJob("11111111-1111-1111-1111-111111111111");

        JobStatusResponse expected_status = new JobStatusResponse("DONE", true, 0, null, null);
        assertThat(result).isEqualTo(expected_status);
    }

    @Test
    public void testCancelJob() throws OctopusIOException, OctopusException {
        // create manager with mocked Jobs and other members stubbed
        Octopus octopus = mock(Octopus.class);
        Jobs jobsEngine= mock(Jobs.class);
        when(octopus.jobs()).thenReturn(jobsEngine);
        Map<String, SandboxedJob> sjobs = new HashMap<String, SandboxedJob>();
        SandboxedJob sjob = mock(SandboxedJob.class);
        Job job = mock(Job.class);
        when(sjob.getJob()).thenReturn(job);
        sjobs.put("11111111-1111-1111-1111-111111111111", sjob);
        OctopusManager manager = new OctopusManager(null, octopus, null, sjobs, null, null);

        manager.cancelJob("11111111-1111-1111-1111-111111111111");

        verify(jobsEngine).cancelJob(job);
    }
}
