package com.ziqni.jenkins.plugins.rabbit.trigger;

import hudson.model.Job;
import net.sf.json.JSONObject;

public abstract class JobInfoMapper {

    /**
     * Creates a JSON object of {@link hudson.model.Job} capturing key information.
     *
     * @param job The job to extract information from.
     * @return A JSON object containing key information about the job.
     */
    public static JSONObject createJobInfoJson(Job<?, ?> job) {
        JSONObject jobInfo = new JSONObject();

        // Capture basic information about the job
        jobInfo.put("name", job.getName());
        jobInfo.put("displayName", job.getDisplayName());
        jobInfo.put("url", job.getUrl());
        jobInfo.put("isBuildable", job.isBuildable());
        jobInfo.put("nextBuildNumber", job.getNextBuildNumber());
        jobInfo.put("buildStatusUrl", job.getBuildStatusUrl());
        jobInfo.put("estimatedDuration", job.getEstimatedDuration());

        return jobInfo;
    }
}
