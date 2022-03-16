package com.kpmg.rcm.sourcing.common.azure;

public class QueueInput {

    /**
     * Azure container name
     */
    public String container;

    /**
     * blob name or file path in azure
     */
    public String key;

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
