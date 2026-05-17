package horse_reserved.model.chatbot;

public enum ReservationFlowStep {
    SELECT_ROUTE,
    SELECT_DATE,
    SELECT_TIME,
    SELECT_PEOPLE_COUNT,
    COLLECT_PARTICIPANT,
    CONFIRM_RESERVATION,
    COMPLETED,
    CANCELLED
}