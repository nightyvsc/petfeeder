private DataStorage storage;

public FeedingData(DataStorage storage) {  // Agregar parámetro
    this.storage = storage;
    this.eatingEvents = new ArrayList<>();
    this.lastCheckTime = LocalDateTime.now();
    
    // feedingTimes ya no es necesario si quitaste horarios
}

public void recordFeedingEvent() {
    lastFeedingTime = LocalDateTime.now();
    storage.recordFeedingEvent("feed");  // ← Guardar en DB
    System.out.println("  📝 Alimentación registrada: " + 
        lastFeedingTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
}

public void recordEatingEvent() {
    LocalDateTime now = LocalDateTime.now();
    eatingEvents.add(now);
    storage.recordFeedingEvent("eat");  // ← Guardar en DB
    System.out.println("  📝 Evento de consumo registrado: " + 
        now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
}