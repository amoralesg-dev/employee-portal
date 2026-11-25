package com.rassini.employeeportal.service;

public interface PortalEventCalendarService {

    CalendarDownload generateIcs(Long userId, Long contentId, String lang);

    record CalendarDownload(String filename, String content) {}
}
