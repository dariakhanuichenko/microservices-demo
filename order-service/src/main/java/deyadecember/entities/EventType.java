package deyadecember.entities;

public enum EventType {
    ORDER_CREATED("orders.created"),
    ORDER_CANCELLED("orders.cancelled");

    private final String topic;
    EventType(String topic) { this.topic = topic; }
    public String topic() { return topic; }
}
