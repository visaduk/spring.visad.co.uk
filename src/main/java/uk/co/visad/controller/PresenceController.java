package uk.co.visad.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import uk.co.visad.model.presence.Activity;
import uk.co.visad.service.PresenceService;

@Controller
@Slf4j
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    @MessageMapping("/activity")
    public void updateActivity(Activity activity, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        presenceService.updateActivity(sessionId, activity);
    }

    @MessageMapping("/heartbeat")
    public void handleHeartbeat(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        presenceService.handleHeartbeat(sessionId);
    }
}
