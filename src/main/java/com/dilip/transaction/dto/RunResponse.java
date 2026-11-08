package com.dilip.transaction.dto;
import com.dilip.transaction.entity.PipelineRun; import java.time.Instant;
public record RunResponse(String runId,String sourceFile,String status,Instant startedAt,Instant endedAt,int inputCount,int curatedCount,int rejectedCount,String failureReason){
 public static RunResponse from(PipelineRun r){return new RunResponse(r.getRunId(),r.getSourceFile(),r.getStatus(),r.getStartedAt(),r.getEndedAt(),r.getInputCount(),r.getCuratedCount(),r.getRejectedCount(),r.getFailureReason());}
}