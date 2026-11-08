package com.dilip.transaction.controller;
import com.dilip.transaction.dto.RunResponse; import com.dilip.transaction.entity.*; import com.dilip.transaction.service.PipelineService;
import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*; import java.io.IOException; import java.nio.file.Path; import java.util.List;
@RestController @RequestMapping("/api/pipeline") public class PipelineController {
 private final PipelineService service; public PipelineController(PipelineService s){service=s;}
 @PostMapping("/runs") public RunResponse run(@RequestParam String filePath)throws IOException{return service.process(Path.of(filePath));}
 @GetMapping("/runs/{id}") public ResponseEntity<RunResponse> status(@PathVariable String id){return service.run(id).map(r->ResponseEntity.ok(RunResponse.from(r))).orElse(ResponseEntity.notFound().build());}
 @GetMapping("/runs/{id}/curated") public List<CuratedTransaction> curated(@PathVariable String id){return service.curatedForRun(id);}
 @GetMapping("/runs/{id}/rejects") public List<RejectedTransaction> rejects(@PathVariable String id){return service.rejectsForRun(id);}
}