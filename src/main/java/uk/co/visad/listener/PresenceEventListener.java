package uk.co.visad.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import uk.co.visad.service.PresenceService;
import java.security.Principal;

@Component
@Slf4j
@RequiredArgsConstructor
public class PresenceEventListener {

    private final PresenceService presenceService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();
        
        if (user != null) {
            String sessionId = headerAccessor.getSessionId();
            // Assuming Principal.getName() returns the username. 
            // In AuthChannelInterceptor we set UsernamePasswordAuthenticationToken, so getName() should be valid.
            presenceService.registerSession(sessionId, "u-" + user.getName(), user.getName());
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        presenceService.removeSession(sessionId);
    }
    
    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        Principal user = headerAccessor.getUser();

        if (user != null && "/topic/presence".equals(destination)) {
             presenceService.sendSnapshotToUser(user.getName());
        }
    }
}
