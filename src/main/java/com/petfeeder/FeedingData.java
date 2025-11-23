package com.petfeeder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FeedingData {
    
    private DataStorage storage;
    private LocalDateTime lastFeedingTime;
    private LocalDateTime lastCheckTime;
    private List<LocalDateTime> eatingEvents;
    
    public FeedingData(DataStorage storage) {
        this.storage = storage;
        this.eatingEvents = new ArrayList<>();
        this.lastCheckTime = LocalDateTime.now();
    }
    
    public void recordFeedingEvent() {
        lastFeedingTime = LocalDateTime.now();
        storage.recordFeedingEvent("feed");
        System.out.println("  📝 Alimentación registrada: " + 
            lastFeedingTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
    
    public void recordEatingEvent() {
        LocalDateTime now = LocalDateTime.now();
        eatingEvents.add(now);
        storage.recordFeedingEvent("eat");
        System.out.println("  📝 Evento de consumo registrado: " + 
            now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
    
    public String getLastFeedingTime() {
        if (lastFeedingTime == null) {
            return "Nunca";
        }
        return lastFeedingTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    public String getNextFeedingTime() {
        return "Manual"; // Ya que no hay horarios programados
    }
}