package com.devtalles.medassistant.agent;

import com.devtalles.medassistant.dto.agent.AppointmentSelection;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class PendingBookingStore {

	private final ConcurrentHashMap<Long, AppointmentSelection> pending = new ConcurrentHashMap<>();

	public void store(Long userId, AppointmentSelection selection) {
		pending.put(userId, selection);
	}

	public AppointmentSelection retieve(Long userId) {
		return pending.get(userId);
	}

	public AppointmentSelection remove(Long userId) {
		return pending.remove(userId);
	}
}
