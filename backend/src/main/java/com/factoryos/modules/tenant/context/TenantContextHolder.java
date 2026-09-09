package com.factoryos.modules.tenant.context;

import java.util.Optional;
import java.util.UUID;

public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CONTEXT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void setContext(TenantContext context) {
        CONTEXT.set(context);
    }

    public static Optional<TenantContext> getContext() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static UUID getCurrentPlantId() {
        TenantContext ctx = CONTEXT.get();
        return ctx != null ? ctx.currentPlantId() : null;
    }

    public static boolean isGlobalAdmin() {
        TenantContext ctx = CONTEXT.get();
        return ctx != null && ctx.isGlobalAdmin();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
