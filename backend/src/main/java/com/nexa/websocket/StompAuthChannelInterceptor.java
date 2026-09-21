package com.nexa.websocket;

import com.nexa.config.JwtUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Vérifie le token JWT (envoyé dans l'en-tête STOMP "Authorization" au moment du CONNECT)
 * avant d'accepter une connexion WebSocket. Sans ça, n'importe qui pourrait ouvrir
 * une connexion temps réel sans être authentifié.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    public StompAuthChannelInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            String token = (authHeaders != null && !authHeaders.isEmpty())
                    ? authHeaders.get(0).replace("Bearer ", "")
                    : null;

            if (token == null || !jwtUtil.isValid(token)) {
                throw new IllegalArgumentException("Connexion WebSocket refusée : jeton invalide ou absent.");
            }

            String email = jwtUtil.extractEmail(token);
            var auth = new UsernamePasswordAuthenticationToken(email, null, List.of());
            accessor.setUser(auth);
        }
        return message;
    }
}
