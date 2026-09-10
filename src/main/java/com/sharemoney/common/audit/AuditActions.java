package com.sharemoney.common.audit;

public final class AuditActions {

    private AuditActions() {
    }

    public static final String LOGIN = "LOGIN";
    public static final String LOGOUT = "LOGOUT";
    public static final String UPLOAD_SLIP = "UPLOAD_SLIP";
    public static final String DELETE_SLIP = "DELETE_SLIP";
    public static final String UPLOAD_DOCUMENT = "UPLOAD_DOCUMENT";
    public static final String DELETE_DOCUMENT = "DELETE_DOCUMENT";
    public static final String UPLOAD_AVATAR = "UPLOAD_AVATAR";
    public static final String DELETE_AVATAR = "DELETE_AVATAR";
    public static final String IMPORT_LEGACY_DATA = "IMPORT_LEGACY_DATA";

    public static String withTarget(String action, String target) {
        return action + " -> " + target;
    }
}
