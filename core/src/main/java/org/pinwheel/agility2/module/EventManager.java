package org.pinwheel.agility2.module;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Copyright (C), 2016 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 */
public enum EventManager {

    INSTANCE;

    private final Set<EventReceiver> receiverSet = new HashSet<>();
    private final Queue<Event> eventQueue = new ConcurrentLinkedQueue<>();

    private void handleEvents() {
        if (receiverSet.isEmpty()) {
            return;
        }
        while (!eventQueue.isEmpty()) {
            dispatchEvent(eventQueue.poll());
        }
    }

    private void dispatchEvent(final Event event) {
        if (null != event && null != event.action) {
            for (EventReceiver receiver : receiverSet) {
                receiver.onReceive(event);
            }
        }
    }

    public void post(Event event) {
        if (null == event || null == event.action) {
            return;
        }
        eventQueue.offer(event);
        handleEvents();
    }

    public void postEmpty(String action) {
        post(new Event(action));
    }

    public void register(EventReceiver object) {
        receiverSet.add(object);
        handleEvents();
    }

    public void unregister(EventReceiver object) {
        receiverSet.remove(object);
    }

    public interface EventReceiver {
        void onReceive(Event event);
    }

    public static class Event {
        private String action;
        private Object content;

        public Event(String action, Object obj) {
            this.action = action;
            this.content = obj;
        }

        public Event(String action) {
            this.action = action;
        }

        public Object getContent() {
            return content;
        }

        public String getAction() {
            return action;
        }
    }

}
