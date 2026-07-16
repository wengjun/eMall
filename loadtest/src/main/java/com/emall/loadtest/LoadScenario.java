package com.emall.loadtest;

enum LoadScenario {
    CHECKOUT("checkout"),
    READ_HEAVY("read-heavy"),
    HOT_SKU("hot-sku"),
    PAYMENT_CALLBACKS("payment-callbacks"),
    MQ_BACKLOG("mq-backlog"),
    FLASH_SALE_HOTSPOT("flash-sale-hotspot"),
    PRODUCTION_MIX("production-mix");

    private final String cliName;

    LoadScenario(String cliName) {
        this.cliName = cliName;
    }

    static LoadScenario from(String value) {
        for (LoadScenario scenario : values()) {
            if (scenario.cliName.equalsIgnoreCase(value)) {
                return scenario;
            }
        }
        throw new IllegalArgumentException("unsupported load scenario: " + value);
    }

    String cliName() {
        return cliName;
    }
}
