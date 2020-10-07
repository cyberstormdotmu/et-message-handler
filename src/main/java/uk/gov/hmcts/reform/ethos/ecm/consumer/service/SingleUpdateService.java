package uk.gov.hmcts.reform.ethos.ecm.consumer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ecm.common.client.CcdClient;
import uk.gov.hmcts.ecm.common.helpers.UtilHelper;
import uk.gov.hmcts.ecm.common.model.ccd.CCDRequest;
import uk.gov.hmcts.ecm.common.model.ccd.SubmitEvent;
import uk.gov.hmcts.ecm.common.model.servicebus.UpdateCaseMsg;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class SingleUpdateService {

    private final CcdClient ccdClient;
    private final UserService userService;

    @Autowired
    public SingleUpdateService(CcdClient ccdClient,
                               UserService userService) {
        this.ccdClient = ccdClient;
        this.userService = userService;
    }

    public void sendUpdateToSingleLogic(UpdateCaseMsg updateCaseMsg) throws IOException {

        String accessToken = userService.getAccessToken();

        List<SubmitEvent> submitEvents = retrieveSingleCase(accessToken, updateCaseMsg);
        if (submitEvents != null && !submitEvents.isEmpty()) {

            sendUpdate(submitEvents.get(0), accessToken, updateCaseMsg);

        } else {
            log.info("No submit events found");
        }
    }

    private List<SubmitEvent> retrieveSingleCase(String authToken, UpdateCaseMsg updateCaseMsg) throws IOException {

        return ccdClient.retrieveCasesElasticSearch(authToken,
                                                    UtilHelper.getCaseTypeId(updateCaseMsg.getCaseTypeId()),
                                                    new ArrayList<>(Collections.singletonList(updateCaseMsg.getEthosCaseReference())));

    }

    private void sendUpdate(SubmitEvent submitEvent, String accessToken, UpdateCaseMsg updateCaseMsg) throws IOException {

        String caseTypeId = UtilHelper.getCaseTypeId(updateCaseMsg.getCaseTypeId());
        String jurisdiction = updateCaseMsg.getJurisdiction();
        String caseId = String.valueOf(submitEvent.getCaseId());

        CCDRequest returnedRequest = ccdClient.startEventForCaseAPIRole(accessToken,
                                                                 caseTypeId,
                                                                 jurisdiction,
                                                                 caseId);
        log.info("Sending Update of single case: " + updateCaseMsg);
        updateCaseMsg.runTask(submitEvent);
        log.info("SubmitEventUpdated: " + submitEvent.getCaseData().getMultipleReference());

       ccdClient.submitEventForCase(accessToken,
                                    submitEvent.getCaseData(),
                                    caseTypeId,
                                    jurisdiction,
                                    returnedRequest,
                                    caseId);

    }

}
