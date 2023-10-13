package com.houtong.rpc;

import java.io.Serializable;
import java.util.UUID;

public class NotFoundMarker implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String uniqueMarkerId;

    public NotFoundMarker() {
        this.uniqueMarkerId = UUID.randomUUID().toString();
    }

    public String getUniqueMarkerId() {
        return uniqueMarkerId;
    }
}
