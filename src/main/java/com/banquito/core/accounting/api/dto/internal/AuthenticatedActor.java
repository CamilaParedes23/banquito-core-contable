package com.banquito.core.accounting.api.dto.internal;

import java.util.List;

public record AuthenticatedActor(
        String subject,
        String actorType,
        String username,
        String clientId,
        List<String> roles,
        List<String> scopes
) {}
