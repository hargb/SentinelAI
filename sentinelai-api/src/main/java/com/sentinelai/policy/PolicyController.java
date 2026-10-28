
package com.sentinelai.policy;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<PolicyEvaluationResponse> evaluate(
            @RequestBody PolicyEvaluationRequest request) {

        PolicyEvaluationResponse response =
                policyService.evaluate(request);

        return ResponseEntity.ok(response);
    }
}
