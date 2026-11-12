package com.educore.sge.kindergarten.web;

import com.educore.sge.kindergarten.application.YearPromotionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/institution/promotion")
@PreAuthorize("hasRole('DIRECTOR')")
public class YearPromotionController {
    private final YearPromotionService service;
    public YearPromotionController(YearPromotionService service){this.service=service;}

    @GetMapping("/preview")
    public YearPromotionService.Preview preview(@RequestHeader("X-Institution-Id") String tenant,
                                               @RequestParam int targetYear){
        return service.preview(tenant,targetYear);
    }
    @PostMapping("/execute")
    public YearPromotionService.Result execute(@RequestHeader("X-Institution-Id") String tenant,
                                              @RequestBody YearPromotionService.Execute request){
        return service.execute(tenant,request);
    }
}
