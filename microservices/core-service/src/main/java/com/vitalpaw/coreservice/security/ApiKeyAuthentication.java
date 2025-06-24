package com.vitalpaw.coreservice.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

public class ApiKeyAuthentication extends AbstractAuthenticationToken {

    private final Object principal;

    public ApiKeyAuthentication(String apiKey) {
        super(Collections.singletonList(new SimpleGrantedAuthority("ROLE_API")));
        this.principal = apiKey;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}