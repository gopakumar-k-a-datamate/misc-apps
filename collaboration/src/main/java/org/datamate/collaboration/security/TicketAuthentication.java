package org.datamate.collaboration.security;

import io.jsonwebtoken.Claims;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom Authentication token for Delegated Chat Tickets.
 * Represents an authenticated user bound to a specific chat thread.
 */
@Getter
public class TicketAuthentication extends AbstractAuthenticationToken {

    private final TicketPrincipal principal;
    private final transient Claims claims; // Claims is not serializable, mark transient

    public TicketAuthentication(Claims claims) {
        super(extractAuthorities(claims));
        this.claims = claims;
        List<String> permissions = extractPermissions(claims);
        this.principal = new TicketPrincipal(
                claims.getSubject(),
                claims.get("threadId", String.class),
                permissions
        );
        setAuthenticated(true);
    }

    private static List<String> extractPermissions(Claims claims) {
        Object permissionsObj = claims.get("permissions");
        if (!(permissionsObj instanceof List<?> permissions)) {
            return Collections.emptyList();
        }
        return permissions.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    private static Collection<? extends GrantedAuthority> extractAuthorities(Claims claims) {
        return extractPermissions(claims).stream()
                .map(p -> new SimpleGrantedAuthority("ROLE_" + p.toUpperCase()))
                .collect(Collectors.toSet());
    }

    @Override
    public Object getCredentials() {
        return null; // No credentials kept after authentication
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }
    
    public TicketPrincipal getTicketPrincipal() {
        return this.principal;
    }

    public String getSub() {
        return this.principal.getUserId();
    }
    
    public String getThreadId() {
        return this.principal.getThreadId();
    }

    public List<String> getPermissions() {
        return this.principal.getPermissions();
    }
}
